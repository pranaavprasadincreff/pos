package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public OrderPojo create(OrderPojo order, List<String> barcodes) throws ApiException {
        populateNewOrder(order);

        Map<String, ProductPojo> productsByBarcode = fetchProductsByBarcode(barcodes);
        setProductIds(order.getOrderItems(), barcodes, productsByBarcode);
        validateSellingPrices(order.getOrderItems(), barcodes, productsByBarcode);

        boolean fulfillable = isOrderFulfillable(order.getOrderItems());
        if (fulfillable) {
            deductInventoryForItems(order.getOrderItems());
        }

        order.setStatus(resolveOrderStatus(fulfillable));
        return orderApi.createOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(String refId, OrderPojo updateRequest, List<String> barcodes)
            throws ApiException {

        OrderPojo existingOrder = orderApi.getByOrderReferenceId(refId);
        validateOrderEditable(existingOrder);

        restoreInventoryIfRequired(existingOrder);

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(barcodes);
        setProductIds(updateRequest.getOrderItems(), barcodes, productByBarcode);
        validateSellingPrices(updateRequest.getOrderItems(), barcodes, productByBarcode);

        boolean fulfillable = isOrderFulfillable(updateRequest.getOrderItems());
        if (fulfillable) {
            deductInventoryForItems(updateRequest.getOrderItems());
        }

        existingOrder.setOrderItems(updateRequest.getOrderItems());
        existingOrder.setStatus(resolveOrderStatus(fulfillable));

        return orderApi.updateOrder(existingOrder);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String refId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(refId);
        validateOrderEditable(order);

        restoreInventoryIfRequired(order);

        order.setStatus(OrderStatus.CANCELLED.name());
        return orderApi.updateOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public void markInvoiced(String refId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(refId);
        validateInvoicingAllowed(order);

        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    public OrderPojo getByRef(String refId) throws ApiException {
        return orderApi.getByOrderReferenceId(refId);
    }

    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime from,
            ZonedDateTime to,
            int page,
            int size
    ) {
        return orderApi.search(refContains, status, from, to, page, size);
    }

    // ---------------- INVENTORY DECISION HELPERS ----------------

    private boolean isOrderFulfillable(List<OrderItemPojo> items) throws ApiException {
        Map<String, Integer> qtyByProductId = aggregateQuantityByProductId(items);
        return inventoryApi.isSufficientInventoryBulk(qtyByProductId);
    }

    private void deductInventoryForItems(List<OrderItemPojo> items) throws ApiException {
        Map<String, Integer> qtyByProductId = aggregateQuantityByProductId(items);
        inventoryApi.deductInventoryBulk(qtyByProductId);
    }

    private void restoreInventoryIfRequired(OrderPojo order) throws ApiException {
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            return;
        }

        Map<String, Integer> qtyByProductId =
                aggregateQuantityByProductId(order.getOrderItems());

        inventoryApi.incrementInventoryBulk(qtyByProductId);
    }

    // ---------------- VALIDATION & MAPPING ----------------

    private void populateNewOrder(OrderPojo order) {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
    }

    private Map<String, ProductPojo> fetchProductsByBarcode(List<String> barcodes)
            throws ApiException {

        List<String> unique = barcodes.stream().distinct().toList();
        List<ProductPojo> products = productApi.findByBarcodes(unique);

        Map<String, ProductPojo> map = new HashMap<>();
        for (ProductPojo p : products) {
            map.put(p.getBarcode(), p);
        }

        List<String> missing = unique.stream()
                .filter(b -> !map.containsKey(b))
                .toList();

        if (!missing.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missing));
        }

        return map;
    }

    private void setProductIds(
            List<OrderItemPojo> items,
            List<String> barcodes,
            Map<String, ProductPojo> productByBarcode
    ) throws ApiException {

        if (items.size() != barcodes.size()) {
            throw new ApiException("Order items and barcode list size mismatch");
        }

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setProductId(productByBarcode.get(barcodes.get(i)).getId());
        }
    }

    private void validateSellingPrices(
            List<OrderItemPojo> items,
            List<String> barcodes,
            Map<String, ProductPojo> productByBarcode
    ) throws ApiException {

        for (int i = 0; i < items.size(); i++) {
            ProductPojo product = productByBarcode.get(barcodes.get(i));
            if (items.get(i).getSellingPrice() > product.getMrp()) {
                throw new ApiException("Selling price exceeds MRP for barcode: " + product.getBarcode());
            }
        }
    }

    private Map<String, Integer> aggregateQuantityByProductId(List<OrderItemPojo> items) {
        Map<String, Integer> map = new HashMap<>();
        for (OrderItemPojo item : items) {
            map.merge(item.getProductId(), item.getOrderedQuantity(), Integer::sum);
        }
        return map;
    }

    private String resolveOrderStatus(boolean fulfillable) {
        return fulfillable
                ? OrderStatus.FULFILLABLE.name()
                : OrderStatus.UNFULFILLABLE.name();
    }

    private void validateOrderEditable(OrderPojo order) throws ApiException {
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())
                || OrderStatus.INVOICED.name().equals(order.getStatus())) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private String generateUniqueOrderReferenceId() {
        for (int i = 0; i < MAX_REF_GENERATION_ATTEMPTS; i++) {
            String ref = "ORD-%04d-%04d".formatted(
                    (int) (Math.random() * 10000),
                    (int) (Math.random() * 10000)
            );
            if (!orderApi.orderReferenceIdExists(ref)) {
                return ref;
            }
        }
        throw new RuntimeException("Unable to generate unique order reference ID");
    }
}
