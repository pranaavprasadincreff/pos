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
    public OrderPojo create(OrderPojo orderToCreate, List<String> barcodesByItemIndex) throws ApiException {
        populateNewOrder(orderToCreate);

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(barcodesByItemIndex);
        setProductIds(orderToCreate.getOrderItems(), barcodesByItemIndex, productByBarcode);
        validateSellingPrices(orderToCreate.getOrderItems(), barcodesByItemIndex, productByBarcode);

        boolean fulfillable = deductInventory(orderToCreate.getOrderItems());
        setOrderStatus(orderToCreate, fulfillable);

        return orderApi.createOrder(orderToCreate);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(String orderReferenceId, OrderPojo updateRequest, List<String> barcodesByItemIndex) throws ApiException {
        OrderPojo existingOrder = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderEditable(existingOrder);

        restoreInventory(existingOrder);

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(barcodesByItemIndex);
        setProductIds(updateRequest.getOrderItems(), barcodesByItemIndex, productByBarcode);
        validateSellingPrices(updateRequest.getOrderItems(), barcodesByItemIndex, productByBarcode);

        boolean fulfillable = deductInventory(updateRequest.getOrderItems());
        applyOrderUpdate(existingOrder, updateRequest, fulfillable);

        return orderApi.updateOrder(existingOrder);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String orderReferenceId) throws ApiException {
        OrderPojo existingOrder = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderEditable(existingOrder);

        restoreInventory(existingOrder);

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

    // ---------------- private helpers ----------------

    private void populateNewOrder(OrderPojo order) {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
    }

    private Map<String, ProductPojo> fetchProductsByBarcode(List<String> barcodes) throws ApiException {
        List<String> uniqueBarcodes = barcodes.stream().distinct().toList();

        List<ProductPojo> products = productApi.findByBarcodes(uniqueBarcodes);

        Map<String, ProductPojo> productByBarcode = new HashMap<>();
        for (ProductPojo p : products) {
            productByBarcode.put(p.getBarcode(), p);
        }

        List<String> missing = uniqueBarcodes.stream()
                .filter(b -> !productByBarcode.containsKey(b))
                .toList();

        if (!missing.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missing));
        }

        return productByBarcode;
    }

    private void setProductIds(
            List<OrderItemPojo> items,
            List<String> barcodesByItemIndex,
            Map<String, ProductPojo> productByBarcode
    ) throws ApiException {

        if (items.size() != barcodesByItemIndex.size()) {
            throw new ApiException("Order items and barcode list size mismatch");
        }

        for (int i = 0; i < items.size(); i++) {
            String barcode = barcodesByItemIndex.get(i);
            ProductPojo product = productByBarcode.get(barcode);
            items.get(i).setProductId(product.getId());
        }
    }

    private void validateSellingPrices(
            List<OrderItemPojo> items,
            List<String> barcodesByItemIndex,
            Map<String, ProductPojo> productByBarcode
    ) throws ApiException {

        for (int i = 0; i < items.size(); i++) {
            String barcode = barcodesByItemIndex.get(i);
            ProductPojo product = productByBarcode.get(barcode);

            if (items.get(i).getSellingPrice() > product.getMrp()) {
                throw new ApiException("Selling price cannot exceed MRP for barcode: " + barcode);
            }
        }
    }

    private boolean deductInventory(List<OrderItemPojo> items) throws ApiException {
        Map<String, Integer> quantityToDeductByProductId = aggregateQuantityByProductId(items);

        // If bulk deduct fails, InventoryApi should throw ApiException.
        inventoryApi.deductInventoryBulk(quantityToDeductByProductId);

        return true;
    }

    private void restoreInventory(OrderPojo order) throws ApiException {
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            return;
        }

        Map<String, Integer> quantityToAddByProductId = aggregateQuantityByProductId(order.getOrderItems());
        inventoryApi.incrementInventoryBulk(quantityToAddByProductId);
    }

    private Map<String, Integer> aggregateQuantityByProductId(List<OrderItemPojo> items) {
        Map<String, Integer> totalByProductId = new HashMap<>();
        for (OrderItemPojo item : items) {
            totalByProductId.merge(item.getProductId(), item.getOrderedQuantity(), Integer::sum);
        }
        return totalByProductId;
    }

    private void setOrderStatus(OrderPojo order, boolean fulfillable) {
        order.setStatus(fulfillable ? OrderStatus.FULFILLABLE.name() : OrderStatus.UNFULFILLABLE.name());
    }

    private void applyOrderUpdate(OrderPojo existingOrder, OrderPojo updateRequest, boolean fulfillable) {
        existingOrder.setOrderItems(updateRequest.getOrderItems());
        setOrderStatus(existingOrder, fulfillable);
    }

    private void validateOrderEditable(OrderPojo order) throws ApiException {
        if (OrderStatus.CANCELLED.name().equals(order.getStatus()) || OrderStatus.INVOICED.name().equals(order.getStatus())) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (OrderStatus.INVOICED.name().equals(order.getStatus())) {
            throw new ApiException("Order already invoiced");
        }
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private String generateUniqueOrderReferenceId() {
        int attempts = 0;

        while (true) {
            attempts++;
            if (attempts > MAX_REF_GENERATION_ATTEMPTS) {
                throw new RuntimeException("Unable to generate unique order ID after 10 attempts");
            }

            String ref = generateRandomOrderReferenceId();
            if (!orderApi.orderReferenceIdExists(ref)) {
                return ref;
            }
        }
    }

    private String generateRandomOrderReferenceId() {
        String part1 = String.format("%04d", (int) (Math.random() * 10000));
        String part2 = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD-" + part1 + "-" + part2;
    }
}
