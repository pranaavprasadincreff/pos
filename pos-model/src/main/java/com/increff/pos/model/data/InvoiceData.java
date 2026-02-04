package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceData {
    private String orderReferenceId;
    private String pdfBase64;
    private String pdfPath;
}

