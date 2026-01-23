package com.increff.invoice.helper;

import com.increff.invoice.db.InvoicePojo;
import com.increff.invoice.modal.data.InvoiceData;

public class InvoiceHelper {

    public static InvoiceData convertToData(InvoicePojo invoicePojo) {
        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId(invoicePojo.getOrderReferenceId());
        data.setPdfBase64(invoicePojo.getPdfBase64());
        data.setPdfPath(invoicePojo.getPdfPath());
        return data;
    }
}
