package com.increff.pos.wrapper;

import com.increff.invoice.client.InvoiceClient;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceClientWrapper {

    @Autowired
    private InvoiceClient invoiceClient;

    public InvoiceData generateInvoice(InvoiceGenerateForm form) throws ApiException {
        return invoiceClient.generateInvoice(form);
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        return invoiceClient.getInvoice(orderReferenceId);
    }
}
