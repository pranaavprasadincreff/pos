package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.client.InvoiceClient;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {

    private final InvoiceClient invoiceClient;
    private final OrderApi orderApi;

    public InvoiceDto(
            InvoiceClient invoiceClient,
            OrderApi orderApi
    ) {
        this.invoiceClient = invoiceClient;
        this.orderApi = orderApi;
    }

    public InvoiceData generateInvoice(String orderReferenceId)
            throws ApiException {

        InvoiceData invoice =
                invoiceClient.generateInvoice(orderReferenceId);

        orderApi.markOrderInvoiced(orderReferenceId);

        return invoice;
    }

    public InvoiceData getInvoice(String orderReferenceId)
            throws ApiException {

        return invoiceClient.getInvoice(orderReferenceId);
    }
}

