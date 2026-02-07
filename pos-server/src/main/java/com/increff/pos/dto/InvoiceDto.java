package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.wrapper.InvoiceClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {

    @Autowired
    private InvoiceFlow invoiceFlow;

    @Autowired
    private InvoiceClientWrapper invoiceClientWrapper;

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = NormalizationUtil.normalizeBarcode(orderReferenceId);

        boolean invoiceExists = invoiceFlow.invoiceAlreadyExists(normalizedOrderReferenceId);
        if (invoiceExists) {
            return invoiceClientWrapper.getInvoice(normalizedOrderReferenceId);
        }

        InvoiceGenerateForm invoiceRequest = invoiceFlow.prepareInvoiceRequest(normalizedOrderReferenceId);
        InvoiceData generatedInvoice = invoiceClientWrapper.generateInvoice(invoiceRequest);
        invoiceFlow.markOrderInvoiced(normalizedOrderReferenceId);
        return generatedInvoice;
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = NormalizationUtil.normalizeBarcode(orderReferenceId);
        return invoiceClientWrapper.getInvoice(normalizedOrderReferenceId);
    }
}
