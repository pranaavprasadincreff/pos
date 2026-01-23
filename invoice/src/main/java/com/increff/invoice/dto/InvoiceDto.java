package com.increff.invoice.dto;

import com.increff.invoice.api.InvoiceApi;
import com.increff.invoice.db.InvoicePojo;
import com.increff.invoice.exception.ApiException;
import com.increff.invoice.helper.InvoiceHelper;
import com.increff.invoice.modal.data.InvoiceData;
import com.increff.invoice.modal.form.InvoiceGenerateForm;
import com.increff.invoice.util.ValidationUtil;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {
    private final InvoiceApi invoiceApi;

    public InvoiceDto(InvoiceApi invoiceApi) {
        this.invoiceApi = invoiceApi;
    }

    public InvoiceData generateInvoice(InvoiceGenerateForm form) throws ApiException {
        ValidationUtil.validateInvoiceGenerateForm(form);
        InvoicePojo invoice = invoiceApi.generateInvoice(form);
        return InvoiceHelper.convertToData(invoice);
    }

    public InvoiceData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        ValidationUtil.validateOrderReferenceId(orderReferenceId);
        InvoicePojo invoice = invoiceApi.getByOrderReferenceId(orderReferenceId);
        return InvoiceHelper.convertToData(invoice);
    }
}
