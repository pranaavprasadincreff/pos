package com.increff.pos.flow;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;
import com.increff.pos.wrapper.InvoiceClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceFlow {
    @Autowired
    // TODO move to dto
    private InvoiceClientWrapper invoiceClientWrapper;
    @Autowired
    private OrderApi orderApi;
    @Autowired
    private ProductApi productApi;

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        InvoiceData existingInvoice = fetchInvoiceForInvoicedOrder(order);
        if (existingInvoice != null) {
            return existingInvoice;
        }
        validateOrderEligibleForInvoicing(order);
        InvoiceGenerateForm invoiceRequest = buildInvoiceRequest(order);
        InvoiceData generatedInvoice = invoiceClientWrapper.generateInvoice(invoiceRequest);
        markOrderInvoiced(order);
        return generatedInvoice;
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        return invoiceClientWrapper.getInvoice(orderReferenceId);
    }

    private InvoiceData fetchInvoiceForInvoicedOrder(OrderPojo order) throws ApiException {
        if (!isOrderInvoiced(order)) {
            return null;
        }
        return invoiceClientWrapper.getInvoice(order.getOrderReferenceId());
    }

    private void validateOrderEligibleForInvoicing(OrderPojo order) throws ApiException {
        if (order == null) {
            throw new ApiException("Order not found");
        }
        if (!isOrderFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private InvoiceGenerateForm buildInvoiceRequest(OrderPojo order) throws ApiException {
        Map<String, ProductPojo> productByBarcode = loadProductsByBarcode(order);
        List<InvoiceItemForm> invoiceItems = order.getOrderItems()
                .stream()
                .map(orderItem -> buildInvoiceItem(orderItem, productByBarcode))
                .toList();

        InvoiceGenerateForm invoiceRequest = new InvoiceGenerateForm();
        invoiceRequest.setOrderReferenceId(order.getOrderReferenceId());
        invoiceRequest.setItems(invoiceItems);
        return invoiceRequest;
    }

    private Map<String, ProductPojo> loadProductsByBarcode(OrderPojo order) throws ApiException {
        List<String> orderedBarcodes = getDistinctOrderedBarcodes(order);
        List<ProductPojo> products = productApi.findByBarcodes(orderedBarcodes);
        Map<String, ProductPojo> productByBarcode = products.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        validateAllProductsPresent(orderedBarcodes, productByBarcode);
        return productByBarcode;
    }

    private List<String> getDistinctOrderedBarcodes(OrderPojo order) {
        if (order == null || order.getOrderItems() == null) {
            return List.of();
        }
        return order.getOrderItems()
                .stream()
                .map(OrderItemPojo::getProductBarcode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void validateAllProductsPresent(List<String> orderedBarcodes, Map<String, ProductPojo> productByBarcode) throws ApiException {
        Set<String> missingBarcodes = new LinkedHashSet<>();
        for (String barcode : orderedBarcodes) {
            if (!productByBarcode.containsKey(barcode)) {
                missingBarcodes.add(barcode);
            }
        }

        if (!missingBarcodes.isEmpty()) {
            throw new ApiException("Missing product data for barcodes: " + String.join(", ", missingBarcodes));
        }
    }

    private InvoiceItemForm buildInvoiceItem(OrderItemPojo orderItem, Map<String, ProductPojo> productByBarcode) {
        ProductPojo product = productByBarcode.get(orderItem.getProductBarcode());

        InvoiceItemForm invoiceItem = new InvoiceItemForm();
        invoiceItem.setBarcode(product.getBarcode());
        invoiceItem.setProductName(product.getName());
        invoiceItem.setQuantity(orderItem.getOrderedQuantity());
        invoiceItem.setSellingPrice(orderItem.getSellingPrice());
        return invoiceItem;
    }

    private void markOrderInvoiced(OrderPojo order) throws ApiException {
        validateInvoicingAllowed(order);
        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (isOrderInvoiced(order)) {
            throw new ApiException("Order already invoiced");
        }
        if (!isOrderFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private boolean isOrderInvoiced(OrderPojo order) {
        return order != null && OrderStatus.INVOICED.name().equals(order.getStatus());
    }

    private boolean isOrderFulfillable(OrderPojo order) {
        return order != null && OrderStatus.FULFILLABLE.name().equals(order.getStatus());
    }
}
