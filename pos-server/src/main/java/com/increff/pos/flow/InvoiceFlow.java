package com.increff.pos.flow;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceFlow {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private ProductApi productApi;

    public boolean invoiceAlreadyExists(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        return isOrderInvoiced(order);
    }

    public InvoiceGenerateForm prepareInvoiceRequest(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);

        if (isOrderInvoiced(order)) {
            throw new ApiException("Invoice already exists for order: " + orderReferenceId);
        }

        validateOrderEligibleForInvoicing(order);
        return buildInvoiceRequest(order);
    }

    public void markOrderInvoiced(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);

        validateInvoicingAllowed(order);

        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    // ---------------- private helpers ----------------

    private void validateOrderEligibleForInvoicing(OrderPojo order) throws ApiException {
        if (order == null) {
            throw new ApiException("Order not found");
        }
        if (isNotOrderFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new ApiException("Order has no items");
        }
    }

    private InvoiceGenerateForm buildInvoiceRequest(OrderPojo order) throws ApiException {
        Map<String, ProductPojo> productById = loadProductsById(order);

        List<InvoiceItemForm> invoiceItems = new ArrayList<>();
        for (OrderItemPojo orderItem : order.getOrderItems()) {
            invoiceItems.add(buildInvoiceItem(orderItem, productById));
        }

        InvoiceGenerateForm invoiceRequest = new InvoiceGenerateForm();
        invoiceRequest.setOrderReferenceId(order.getOrderReferenceId());
        invoiceRequest.setItems(invoiceItems);
        return invoiceRequest;
    }

    private Map<String, ProductPojo> loadProductsById(OrderPojo order) throws ApiException {
        List<String> orderedProductIds = getDistinctOrderedProductIds(order);

        List<ProductPojo> products = productApi.findByIds(orderedProductIds);
        Map<String, ProductPojo> productById = products.stream()
                .collect(Collectors.toMap(ProductPojo::getId, Function.identity()));

        validateAllProductsPresent(orderedProductIds, productById);
        return productById;
    }

    private List<String> getDistinctOrderedProductIds(OrderPojo order) throws ApiException {
        Set<String> ids = new LinkedHashSet<>();

        for (OrderItemPojo item : order.getOrderItems()) {
            String productId = item == null ? null : item.getProductId();
            if (productId == null) {
                throw new ApiException("Order item missing productId");
            }
            ids.add(productId);
        }

        return new ArrayList<>(ids);
    }

    private void validateAllProductsPresent(List<String> orderedProductIds, Map<String, ProductPojo> productById) throws ApiException {
        Set<String> missingProductIds = new LinkedHashSet<>();
        for (String productId : orderedProductIds) {
            if (!productById.containsKey(productId)) {
                missingProductIds.add(productId);
            }
        }

        if (!missingProductIds.isEmpty()) {
            throw new ApiException("Missing product data for productIds: " + String.join(", ", missingProductIds));
        }
    }

    private InvoiceItemForm buildInvoiceItem(OrderItemPojo orderItem, Map<String, ProductPojo> productById) throws ApiException {
        if (orderItem == null) {
            throw new ApiException("Invalid order item");
        }

        String productId = orderItem.getProductId();
        if (productId == null) {
            throw new ApiException("Order item missing productId");
        }

        ProductPojo product = productById.get(productId);
        if (product == null) {
            throw new ApiException("Product not found for productId: " + productId);
        }

        InvoiceItemForm invoiceItem = new InvoiceItemForm();
        invoiceItem.setBarcode(product.getBarcode()); // invoice service expects barcode
        invoiceItem.setProductName(product.getName());
        invoiceItem.setQuantity(orderItem.getOrderedQuantity());
        invoiceItem.setSellingPrice(orderItem.getSellingPrice());
        return invoiceItem;
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (isOrderInvoiced(order)) {
            throw new ApiException("Order already invoiced");
        }
        if (isNotOrderFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private boolean isOrderInvoiced(OrderPojo order) {
        return order != null && OrderStatus.INVOICED.name().equals(order.getStatus());
    }

    private boolean isNotOrderFulfillable(OrderPojo order) {
        return order == null || !OrderStatus.FULFILLABLE.name().equals(order.getStatus());
    }
}
