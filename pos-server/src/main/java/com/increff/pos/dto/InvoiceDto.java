package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.util.NormalizationUtil;
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
        String normalized = normalizeOrderRef(orderReferenceId);
        validateOrderRef(normalized);
        return normalized;
    }

    private String normalizeOrderRef(String value) {
        if (!StringUtils.hasText(value)) return value;
        return value.trim().toUpperCase();
    }

    private void validateOrderRef(String value) throws ApiException {
        if (!StringUtils.hasText(value)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        if (value.length() > 50) {
            throw new ApiException("Order reference id too long");
        }
    }
}
