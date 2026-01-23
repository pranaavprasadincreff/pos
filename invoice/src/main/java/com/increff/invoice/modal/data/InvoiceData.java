package com.increff.invoice.modal.data;

import lombok.Data;

@Data
public class InvoiceData {
    private String orderReferenceId;
    private String pdfBase64;
    private String pdfPath;
}

