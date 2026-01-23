package com.increff.pos.model.data;

import lombok.Data;

@Data
public class InvoiceData {
    private String orderReferenceId;
    private String pdfBase64;
    private String pdfPath;
}

