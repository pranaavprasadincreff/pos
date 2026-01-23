package com.increff.invoice.util;

import com.increff.invoice.exception.ApiException;
import com.increff.invoice.modal.form.InvoiceGenerateForm;
import org.springframework.util.StringUtils;

public class ValidationUtil {
    public static void validateInvoiceGenerateForm(InvoiceGenerateForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Invoice generate form cannot be null");
        }
        validateOrderReferenceId(form.getOrderReferenceId());
    }

    public static void validateOrderReferenceId(String orderReferenceId) throws ApiException {
        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
    }
}
