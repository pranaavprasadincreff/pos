package com.increff.pos.flow;

import com.increff.pos.api.ProductApi;
import com.increff.pos.wrapper.InvoiceClientWrapper;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceFlow {
    @Autowired
    private InvoiceClientWrapper invoiceClientWrapper;
    @Autowired
    private OrderFlow orderFlow;
    @Autowired
    private ProductApi productApi;

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        OrderPojo order = orderFlow.getByRef(orderReferenceId);
        InvoiceData existing = tryReturnExistingInvoice(order);
        if (existing != null) return existing;
        validateInvoiceAllowed(order);
        InvoiceGenerateForm form = buildInvoiceForm(order);
        InvoiceData data = invoiceClientWrapper.generateInvoice(form);
        orderFlow.markInvoiced(orderReferenceId);
        return data;
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        return invoiceClientWrapper.getInvoice(orderReferenceId);
    }

    private InvoiceData tryReturnExistingInvoice(OrderPojo order) throws ApiException {
        if (!isInvoiced(order)) return null;
        return invoiceClientWrapper.getInvoice(order.getOrderReferenceId());
    }

    private void validateInvoiceAllowed(OrderPojo order) throws ApiException {
        if (!isFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private InvoiceGenerateForm buildInvoiceForm(OrderPojo order) throws ApiException {
        Map<String, ProductPojo> productByBarcode = loadProducts(order);
        List<InvoiceItemForm> items = order.getOrderItems()
                .stream()
                .map(i -> toInvoiceItem(i, productByBarcode))
                .toList();

        InvoiceGenerateForm form = new InvoiceGenerateForm();
        form.setOrderReferenceId(order.getOrderReferenceId());
        form.setItems(items);
        return form;
    }

    private Map<String, ProductPojo> loadProducts(OrderPojo order) throws ApiException {
        List<String> barcodes = order.getOrderItems()
                .stream()
                .map(OrderItemPojo::getProductBarcode)
                .distinct()
                .toList();

        Map<String, ProductPojo> map = productApi.findByBarcodes(barcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
        validateAllProductsFound(barcodes, map);
        return map;
    }

    private void validateAllProductsFound(List<String> barcodes, Map<String, ProductPojo> map)
            throws ApiException {
        Set<String> missing = barcodes.stream()
                .filter(b -> !map.containsKey(b))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new ApiException("Missing product data for barcodes: " + String.join(", ", missing));
        }
    }

    private InvoiceItemForm toInvoiceItem(
            com.increff.pos.db.OrderItemPojo item,
            Map<String, ProductPojo> productByBarcode
    ) {
        ProductPojo p = productByBarcode.get(item.getProductBarcode());

        InvoiceItemForm f = new InvoiceItemForm();
        f.setBarcode(p.getBarcode());
        f.setProductName(p.getName());
        f.setQuantity(item.getOrderedQuantity());
        f.setSellingPrice(item.getSellingPrice());
        return f;
    }

    private boolean isInvoiced(OrderPojo order) {
        return OrderStatus.INVOICED.name().equals(order.getStatus());
    }

    private boolean isFulfillable(OrderPojo order) {
        return OrderStatus.FULFILLABLE.name().equals(order.getStatus());
    }
}
