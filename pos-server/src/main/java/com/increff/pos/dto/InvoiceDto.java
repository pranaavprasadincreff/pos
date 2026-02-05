package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.util.NormalizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {
    @Autowired
    private InvoiceFlow invoiceFlow;

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = normalizeOrderReferenceId(orderReferenceId);
        return invoiceFlow.generateInvoice(normalizedOrderReferenceId);
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = normalizeOrderReferenceId(orderReferenceId);
        return invoiceFlow.getInvoice(normalizedOrderReferenceId);
    }

    private String normalizeOrderReferenceId(String orderReferenceId) {
        return NormalizationUtil.normalizeBarcode(orderReferenceId);
    }
}
