package com.increff.pos.wrapper;

import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;

public interface InvoiceClientWrapper {
    InvoiceData generateInvoice(InvoiceGenerateForm form) throws ApiException;
    InvoiceData getInvoice(String orderReferenceId) throws ApiException;
}
