package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderFlow {

    @Autowired
    private OrderApi orderApi;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private ProductApi productApi;

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo create(OrderPojo order) throws ApiException {
        populateNewOrderFields(order);
        validateSellingPriceWithinMrp(order.getOrderItems());
        boolean fulfillable = tryDeductInventory(order.getOrderItems());
        setStatusByFulfillability(order, fulfillable);
        return orderApi.createOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(String ref, OrderPojo updated) throws ApiException {
        OrderPojo existing = orderApi.getByOrderReferenceId(ref);
        validateEditable(existing);
        restoreInventoryIfNeeded(existing);
        validateSellingPriceWithinMrp(existing.getOrderItems());
        boolean fulfillable = tryDeductInventory(updated.getOrderItems());
        applyUpdate(existing, updated, fulfillable);
        return orderApi.updateOrder(existing);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String ref) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(ref);
        validateEditable(order);
        restoreInventoryIfNeeded(order);
        order.setStatus(OrderStatus.CANCELLED.name());
        return orderApi.updateOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public void markInvoiced(String ref) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(ref);
        validateInvoicingAllowed(order);
        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    public OrderPojo getByRef(String ref) throws ApiException {
        return orderApi.getByOrderReferenceId(ref);
    }

    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    ) {
        return orderApi.search(refContains, status, fromTime, toTime, page, size);
    }

    // ---------------- private helpers ----------------

    private void populateNewOrderFields(OrderPojo order) {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
    }

    private boolean tryDeductInventory(List<OrderItemPojo> items) throws ApiException {
        InventoryContext ctx = buildInventoryContext(items);

        if (!hasSufficientInventory(items, ctx)) return false;

        deductInventory(items, ctx);
        return true;
    }

    private void restoreInventoryIfNeeded(OrderPojo order) throws ApiException {
        if (isUnfulfillable(order)) return;

        InventoryContext ctx = buildInventoryContext(order.getOrderItems());
        incrementInventory(order.getOrderItems(), ctx);
    }

    private InventoryContext buildInventoryContext(List<OrderItemPojo> items) throws ApiException {
        Map<String, String> productIdByBarcode = getProductIdByBarcode(items);
        Map<String, InventoryPojo> inventoryByProductId =
                getInventoryByProductId(productIdByBarcode.values());

        return new InventoryContext(productIdByBarcode, inventoryByProductId);
    }

    private Map<String, String> getProductIdByBarcode(List<OrderItemPojo> items) throws ApiException {
        List<String> barcodes = items.stream()
                .map(OrderItemPojo::getProductBarcode)
                .distinct()
                .toList();

        List<ProductPojo> products = productApi.findByBarcodes(barcodes);
        Map<String, ProductPojo> productByBarcode = products.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        Set<String> missing = barcodes.stream()
                .filter(b -> !productByBarcode.containsKey(b))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missing));
        }

        Map<String, String> map = new HashMap<>();
        for (String b : barcodes) {
            map.put(b, productByBarcode.get(b).getId());
        }
        return map;
    }

    private Map<String, InventoryPojo> getInventoryByProductId(Collection<String> productIds) {
        List<String> ids = productIds.stream().distinct().toList();
        return inventoryApi.getByProductIds(ids).stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));
    }

    private boolean hasSufficientInventory(List<OrderItemPojo> items, InventoryContext ctx) {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQty(items, ctx);

        for (Map.Entry<String, Integer> e : requiredQtyByProductId.entrySet()) {
            InventoryPojo inv = ctx.inventoryByProductId().get(e.getKey());
            int available = inv == null || inv.getQuantity() == null ? 0 : inv.getQuantity();
            if (available < e.getValue()) return false;
        }

        return true;
    }

    private void deductInventory(List<OrderItemPojo> items, InventoryContext ctx) throws ApiException {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQty(items, ctx);

        for (Map.Entry<String, Integer> e : requiredQtyByProductId.entrySet()) {
            inventoryApi.deductInventory(e.getKey(), e.getValue());
        }
    }

    private void incrementInventory(List<OrderItemPojo> items, InventoryContext ctx) throws ApiException {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQty(items, ctx);

        for (Map.Entry<String, Integer> e : requiredQtyByProductId.entrySet()) {
            inventoryApi.incrementInventory(e.getKey(), e.getValue());
        }
    }

    private Map<String, Integer> aggregateRequiredQty(List<OrderItemPojo> items, InventoryContext ctx) {
        Map<String, Integer> map = new HashMap<>();
        for (OrderItemPojo item : items) {
            String productId = ctx.productIdByBarcode().get(item.getProductBarcode());
            map.put(productId, map.getOrDefault(productId, 0) + item.getOrderedQuantity());
        }
        return map;
    }

    private void setStatusByFulfillability(OrderPojo order, boolean fulfillable) {
        order.setStatus(fulfillable
                ? OrderStatus.FULFILLABLE.name()
                : OrderStatus.UNFULFILLABLE.name());
    }

    private void applyUpdate(OrderPojo existing, OrderPojo updated, boolean fulfillable) {
        existing.setOrderItems(updated.getOrderItems());
        setStatusByFulfillability(existing, fulfillable);
    }

    private void validateEditable(OrderPojo order) throws ApiException {
        if (isCancelled(order) || isInvoiced(order)) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (isInvoiced(order)) throw new ApiException("Order already invoiced");
        if (isUnfulfillable(order)) throw new ApiException("Only fulfillable orders can be invoiced");
    }

    private boolean isUnfulfillable(OrderPojo o) {
        return !OrderStatus.FULFILLABLE.name().equals(o.getStatus());
    }

    private boolean isCancelled(OrderPojo o) {
        return OrderStatus.CANCELLED.name().equals(o.getStatus());
    }

    private boolean isInvoiced(OrderPojo o) {
        return OrderStatus.INVOICED.name().equals(o.getStatus());
    }

    private String generateUniqueOrderReferenceId() {
        String ref;
        int attempts = 0;

        do {
            attempts++;
            validateRefGenerationAttempts(attempts);
            ref = randomRef();
        } while (orderApi.orderReferenceIdExists(ref));

        return ref;
    }

    private void validateRefGenerationAttempts(int attempts) {
        if (attempts > 10) {
            throw new RuntimeException("Unable to generate unique order ID after 10 attempts");
        }
    }

    private String randomRef() {
        String part1 = String.format("%04d", (int) (Math.random() * 10000));
        String part2 = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD-" + part1 + "-" + part2;
    }

    private record InventoryContext(
            Map<String, String> productIdByBarcode,
            Map<String, InventoryPojo> inventoryByProductId
    ) {}

    private void validateSellingPriceWithinMrp(List<OrderItemPojo> items) throws ApiException {
        Map<String, ProductPojo> productByBarcode = getProductsByBarcode(items);
        for (OrderItemPojo item : items) {
            String barcode = item.getProductBarcode();
            ProductPojo p = productByBarcode.get(barcode);
            if (p == null) {
                throw new ApiException("Product not found for barcode: " + barcode);
            }
            if (item.getSellingPrice() > p.getMrp()) {
                throw new ApiException(
                        "Selling price cannot exceed MRP for barcode: " + barcode
                );
            }
        }
    }

    private Map<String, ProductPojo> getProductsByBarcode(List<OrderItemPojo> items) throws ApiException {
        List<String> barcodes = items.stream()
                .map(OrderItemPojo::getProductBarcode)
                .distinct()
                .toList();
        List<ProductPojo> products = productApi.findByBarcodes(barcodes);
        Map<String, ProductPojo> map = products.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
        Set<String> missing = barcodes.stream()
                .filter(b -> !map.containsKey(b))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missing));
        }
        return map;
    }

}
