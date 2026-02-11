package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.subdocs.OrderItemPojo;
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

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ProductApi productApi;

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo create(OrderPojo orderRequest) throws ApiException {
        validateSellingPricesAgainstMrp(orderRequest.getOrderItems());

        boolean fulfillable = isOrderFulfillable(orderRequest.getOrderItems());
        if (fulfillable) {
            deductInventoryForItems(orderRequest.getOrderItems());
        }

        orderRequest.setStatus(resolveOrderStatus(fulfillable));
        return orderApi.createOrder(orderRequest);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(OrderPojo updateRequest) throws ApiException {
        String orderReferenceId = updateRequest.getOrderReferenceId();
        OrderPojo existingOrder = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderEditable(existingOrder);
        restoreInventoryIfRequired(existingOrder);

        validateSellingPricesAgainstMrp(updateRequest.getOrderItems());
        boolean fulfillable = isOrderFulfillable(updateRequest.getOrderItems());
        if (fulfillable) {
            deductInventoryForItems(updateRequest.getOrderItems());
        }

        existingOrder.setOrderItems(updateRequest.getOrderItems());
        existingOrder.setStatus(resolveOrderStatus(fulfillable));
        return orderApi.updateOrder(existingOrder);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        validateOrderEditable(order);
        restoreInventoryIfRequired(order);
        order.setStatus(OrderStatus.CANCELLED.name());
        return orderApi.updateOrder(order);
    }

    public OrderPojo getByRef(String orderReferenceId) throws ApiException {
        return orderApi.getByOrderReferenceId(orderReferenceId);
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

    public Map<String, ProductPojo> getProductsByBarcode(List<String> barcodes) throws ApiException {
        List<ProductPojo> productList = productApi.findByBarcodes(barcodes);

        Map<String, ProductPojo> productByBarcode = new HashMap<>();
        for (ProductPojo product : productList) {
            productByBarcode.put(product.getBarcode(), product);
        }

        List<String> missingBarcodes = barcodes.stream()
                .filter(b -> !productByBarcode.containsKey(b))
                .distinct()
                .toList();

        if (!missingBarcodes.isEmpty()) {
            throw new ApiException("Products not found for barcodes: " + String.join(", ", missingBarcodes));
        }

        return productByBarcode;
    }

    public Map<String, ProductPojo> getProductsByIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();

        List<ProductPojo> productList = productApi.findByIds(productIds);

        Map<String, ProductPojo> productById = new HashMap<>();
        for (ProductPojo product : productList) {
            productById.put(product.getId(), product);
        }

        return productById;
    }

    private void validateSellingPricesAgainstMrp(List<OrderItemPojo> orderItemPojos) throws ApiException {
        List<String> productIds = orderItemPojos.stream()
                .map(OrderItemPojo::getProductId)
                .distinct()
                .toList();

        Map<String, ProductPojo> productById = getProductsByIds(productIds);

        for (OrderItemPojo item : orderItemPojos) {
            ProductPojo product = productById.get(item.getProductId());
            if (product == null) {
                throw new ApiException("Product not found for productId: " + item.getProductId());
            }
            if (item.getSellingPrice() > product.getMrp()) {
                throw new ApiException("Selling price exceeds MRP for barcode: " + product.getBarcode());
            }
        }
    }

    // ================== inventory logic ==================

    private boolean isOrderFulfillable(List<OrderItemPojo> orderItemPojos) throws ApiException {
        Map<String, Integer> requiredQuantityByProductId = aggregateQuantityByProductId(orderItemPojos);
        return inventoryApi.isSufficientInventoryBulk(requiredQuantityByProductId);
    }

    private void deductInventoryForItems(List<OrderItemPojo> orderItemPojos) throws ApiException {
        Map<String, Integer> requiredQuantityByProductId = aggregateQuantityByProductId(orderItemPojos);
        inventoryApi.deductInventoryBulk(requiredQuantityByProductId);
    }

    private void restoreInventoryIfRequired(OrderPojo order) throws ApiException {
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            return;
        }

        Map<String, Integer> requiredQuantityByProductId = aggregateQuantityByProductId(order.getOrderItems());
        inventoryApi.incrementInventoryBulk(requiredQuantityByProductId);
    }

    private Map<String, Integer> aggregateQuantityByProductId(List<OrderItemPojo> orderItemPojos) {
        Map<String, Integer> requiredQuantityByProductId = new HashMap<>();
        for (OrderItemPojo orderItemPojo : orderItemPojos) {
            requiredQuantityByProductId.merge(
                    orderItemPojo.getProductId(),
                    orderItemPojo.getOrderedQuantity(),
                    Integer::sum
            );
        }
        return requiredQuantityByProductId;
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
}
