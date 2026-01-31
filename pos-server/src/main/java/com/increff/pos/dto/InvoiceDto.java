package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.client.InvoiceClient;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceDto {

    private final InvoiceClient invoiceClient;
    private final OrderApi orderApi;
    private final ProductApi productApi;

    public InvoiceDto(
            InvoiceClient invoiceClient,
            OrderApi orderApi,
            ProductApi productApi
    ) {
        this.invoiceClient = invoiceClient;
        this.orderApi = orderApi;
        this.productApi = productApi;
    }

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {

        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        validateInvoiceAllowed(order);

        orderApi.markOrderInvoiced(orderReferenceId);

        InvoiceGenerateForm form = buildInvoiceForm(order);
        return invoiceClient.generateInvoice(form);
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        return invoiceClient.getInvoice(orderReferenceId);
    }

    private void validateInvoiceAllowed(OrderPojo order) throws ApiException {
        if (OrderStatus.INVOICED.name().equals(order.getStatus())) {
            throw new ApiException("Invoice already generated for this order");
        }
        if (!OrderStatus.FULFILLABLE.name().equals(order.getStatus())) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private InvoiceGenerateForm buildInvoiceForm(OrderPojo order) throws ApiException {
        List<InvoiceItemForm> items =
                order.getOrderItems()
                        .stream()
                        .map(this::convertToInvoiceItemForm)
                        .collect(Collectors.toList());

        InvoiceGenerateForm form = new InvoiceGenerateForm();
        form.setOrderReferenceId(order.getOrderReferenceId());
        form.setItems(items);
        return form;
    }

    private InvoiceItemForm convertToInvoiceItemForm(OrderItemPojo item) {
        try {
            var product = productApi.getProductByBarcode(item.getProductBarcode());

            InvoiceItemForm form = new InvoiceItemForm();
            form.setBarcode(product.getBarcode());
            form.setProductName(product.getName());
            form.setQuantity(item.getOrderedQuantity());
            form.setSellingPrice(item.getSellingPrice());
            return form;

        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
