package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InvoiceDto {
    @Autowired
    private InvoiceFlow invoiceFlow;

    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        String ref = normalizeAndValidateRef(orderReferenceId);
        return invoiceFlow.generateInvoice(ref);
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        String ref = normalizeAndValidateRef(orderReferenceId);
        return invoiceFlow.getInvoice(ref);
    }

    private String normalizeAndValidateRef(String orderReferenceId) throws ApiException {
        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        return orderReferenceId.trim().toUpperCase();
    }
}
