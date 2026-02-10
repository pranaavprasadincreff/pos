package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
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
        String normalizedRef = NormalizationUtil.normalizeOrderReferenceId(orderReferenceId);
        ValidationUtil.validateOrderReferenceIdRequired(normalizedRef);

        if (invoiceFlow.invoiceAlreadyExists(normalizedRef)) {
            return invoiceClientWrapper.getInvoice(normalizedRef);
        }

        InvoiceGenerateForm invoiceRequest = invoiceFlow.prepareInvoiceRequest(normalizedRef);
        InvoiceData generated = invoiceClientWrapper.generateInvoice(invoiceRequest);
        invoiceFlow.markOrderInvoiced(normalizedRef);

        return generated;
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        String normalizedRef = NormalizationUtil.normalizeOrderReferenceId(orderReferenceId);
        ValidationUtil.validateOrderReferenceIdRequired(normalizedRef);

        return invoiceClientWrapper.getInvoice(normalizedRef);
    }
}
