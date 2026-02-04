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
    private static final int MAX_REF_GENERATION_ATTEMPTS = 10;

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ProductApi productApi;

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo create(OrderPojo orderToCreate) throws ApiException {
        populateNewOrder(orderToCreate);

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(orderToCreate.getOrderItems());
        validateSellingPrices(orderToCreate.getOrderItems(), productByBarcode);

        boolean isFulfillable = deductInventoryIfPossible(orderToCreate.getOrderItems(), productByBarcode);
        setOrderStatus(orderToCreate, isFulfillable);

        return orderApi.createOrder(orderToCreate);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(String orderReferenceId, OrderPojo updatedOrderRequest) throws ApiException {
        OrderPojo existingOrder = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderIsEditable(existingOrder);

        restoreInventoryIfFulfillable(existingOrder);

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(updatedOrderRequest.getOrderItems());
        validateSellingPrices(updatedOrderRequest.getOrderItems(), productByBarcode);

        boolean isFulfillable = deductInventoryIfPossible(updatedOrderRequest.getOrderItems(), productByBarcode);
        applyOrderUpdate(existingOrder, updatedOrderRequest, isFulfillable);

        return orderApi.updateOrder(existingOrder);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String orderReferenceId) throws ApiException {
        OrderPojo existingOrder = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderIsEditable(existingOrder);

        restoreInventoryIfFulfillable(existingOrder);

        existingOrder.setStatus(OrderStatus.CANCELLED.name());
        return orderApi.updateOrder(existingOrder);
    }

    @Transactional(rollbackFor = ApiException.class)
    public void markInvoiced(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        validateInvoicingAllowed(order);

        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    public OrderPojo getByRef(String orderReferenceId) throws ApiException {
        return orderApi.getByOrderReferenceId(orderReferenceId);
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

    // -------------------- Private helpers --------------------

    private void populateNewOrder(OrderPojo order) {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
    }

    private boolean deductInventoryIfPossible(List<OrderItemPojo> items, Map<String, ProductPojo> productByBarcode) throws ApiException {
        InventoryContext inventoryContext = buildInventoryContext(items, productByBarcode);
        if (!hasSufficientInventory(items, inventoryContext)) {
            return false;
        }
        deductInventory(items, inventoryContext);
        return true;
    }

    private void restoreInventoryIfFulfillable(OrderPojo order) throws ApiException {
        if (!isFulfillable(order)) {
            return;
        }

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(order.getOrderItems());
        InventoryContext inventoryContext = buildInventoryContext(order.getOrderItems(), productByBarcode);

        incrementInventory(order.getOrderItems(), inventoryContext);
    }

    private InventoryContext buildInventoryContext(List<OrderItemPojo> items, Map<String, ProductPojo> productByBarcode) throws ApiException {
        Map<String, String> productIdByBarcode = mapProductIdByBarcode(items, productByBarcode);
        Map<String, InventoryPojo> inventoryByProductId = fetchInventoryByProductId(productIdByBarcode.values());
        return new InventoryContext(productIdByBarcode, inventoryByProductId);
    }

    private Map<String, String> mapProductIdByBarcode(List<OrderItemPojo> items, Map<String, ProductPojo> productByBarcode) throws ApiException {
        List<String> barcodes = items.stream()
                .map(OrderItemPojo::getProductBarcode)
                .distinct()
                .toList();

        validateAllProductsExist(barcodes, productByBarcode);

        Map<String, String> productIdByBarcode = new HashMap<>();
        for (String barcode : barcodes) {
            productIdByBarcode.put(barcode, productByBarcode.get(barcode).getId());
        }
        return productIdByBarcode;
    }

    private Map<String, InventoryPojo> fetchInventoryByProductId(Collection<String> productIds) {
        List<String> uniqueProductIds = productIds.stream().distinct().toList();
        return inventoryApi.getByProductIds(uniqueProductIds).stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));
    }

    private boolean hasSufficientInventory(List<OrderItemPojo> items, InventoryContext inventoryContext) {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQuantity(items, inventoryContext);

        for (Map.Entry<String, Integer> requirement : requiredQtyByProductId.entrySet()) {
            InventoryPojo inventory = inventoryContext.inventoryByProductId().get(requirement.getKey());
            int available = inventory == null || inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            if (available < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void deductInventory(List<OrderItemPojo> items, InventoryContext inventoryContext) throws ApiException {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQuantity(items, inventoryContext);

        for (Map.Entry<String, Integer> requirement : requiredQtyByProductId.entrySet()) {
            inventoryApi.deductInventory(requirement.getKey(), requirement.getValue());
        }
    }

    private void incrementInventory(List<OrderItemPojo> items, InventoryContext inventoryContext) throws ApiException {
        Map<String, Integer> requiredQtyByProductId = aggregateRequiredQuantity(items, inventoryContext);

        for (Map.Entry<String, Integer> requirement : requiredQtyByProductId.entrySet()) {
            inventoryApi.incrementInventory(requirement.getKey(), requirement.getValue());
        }
    }

    private Map<String, Integer> aggregateRequiredQuantity(List<OrderItemPojo> items, InventoryContext inventoryContext) {
        Map<String, Integer> requiredQtyByProductId = new HashMap<>();

        for (OrderItemPojo item : items) {
            String productId = inventoryContext.productIdByBarcode().get(item.getProductBarcode());
            requiredQtyByProductId.put(
                    productId,
                    requiredQtyByProductId.getOrDefault(productId, 0) + item.getOrderedQuantity()
            );
        }
        return requiredQtyByProductId;
    }

    private void setOrderStatus(OrderPojo order, boolean isFulfillable) {
        order.setStatus(isFulfillable ? OrderStatus.FULFILLABLE.name() : OrderStatus.UNFULFILLABLE.name());
    }

    private void applyOrderUpdate(OrderPojo existingOrder, OrderPojo updatedOrderRequest, boolean isFulfillable) {
        existingOrder.setOrderItems(updatedOrderRequest.getOrderItems());
        setOrderStatus(existingOrder, isFulfillable);
    }

    private void validateOrderIsEditable(OrderPojo order) throws ApiException {
        if (isCancelled(order) || isInvoiced(order)) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (isInvoiced(order)) {
            throw new ApiException("Order already invoiced");
        }
        if (!isFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private boolean isFulfillable(OrderPojo order) {
        return OrderStatus.FULFILLABLE.name().equals(order.getStatus());
    }

    private boolean isCancelled(OrderPojo order) {
        return OrderStatus.CANCELLED.name().equals(order.getStatus());
    }

    private boolean isInvoiced(OrderPojo order) {
        return OrderStatus.INVOICED.name().equals(order.getStatus());
    }

    private String generateUniqueOrderReferenceId() {
        int attempts = 0;

        while (true) {
            attempts++;
            validateRefGenerationAttempts(attempts);

            String ref = generateRandomOrderReferenceId();
            if (!orderApi.orderReferenceIdExists(ref)) {
                return ref;
            }
        }
    }

    private void validateRefGenerationAttempts(int attempts) {
        if (attempts > MAX_REF_GENERATION_ATTEMPTS) {
            throw new RuntimeException("Unable to generate unique order ID after 10 attempts");
        }
    }

    private String generateRandomOrderReferenceId() {
        String part1 = String.format("%04d", (int) (Math.random() * 10000));
        String part2 = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD-" + part1 + "-" + part2;
    }

    private void validateSellingPrices(List<OrderItemPojo> items, Map<String, ProductPojo> productByBarcode) throws ApiException {
        for (OrderItemPojo item : items) {
            String barcode = item.getProductBarcode();
            ProductPojo product = productByBarcode.get(barcode);

            if (product == null) {
                throw new ApiException("Product not found for barcode: " + barcode);
            }
            if (item.getSellingPrice() > product.getMrp()) {
                throw new ApiException("Selling price cannot exceed MRP for barcode: " + barcode);
            }
        }
    }

    private Map<String, ProductPojo> fetchProductsByBarcode(List<OrderItemPojo> items) throws ApiException {
        List<String> barcodes = items.stream()
                .map(OrderItemPojo::getProductBarcode)
                .distinct()
                .toList();

        List<ProductPojo> products = productApi.findByBarcodes(barcodes);
        Map<String, ProductPojo> productByBarcode = products.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        validateAllProductsExist(barcodes, productByBarcode);
        return productByBarcode;
    }

    private void validateAllProductsExist(List<String> barcodes, Map<String, ProductPojo> productByBarcode) throws ApiException {
        Set<String> missing = barcodes.stream()
                .filter(b -> !productByBarcode.containsKey(b))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missing));
        }
    }

    private record InventoryContext(
            Map<String, String> productIdByBarcode,
            Map<String, InventoryPojo> inventoryByProductId
    ) {}
}
