package com.increff.invoice.api;

import com.increff.invoice.db.InvoicePojo;
import com.increff.invoice.exception.ApiException;
import com.increff.invoice.modal.form.InvoiceGenerateForm;

public interface InvoiceApi {
    InvoicePojo generateInvoice(InvoiceGenerateForm form) throws ApiException;
    InvoicePojo getByOrderReferenceId(String orderReferenceId) throws ApiException;
}
