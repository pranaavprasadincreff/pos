package com.increff.invoice.modal.form;

import lombok.Data;

@Data
public class InvoiceItemForm {
    private String barcode;
    private String productName;
    private Integer quantity;
    private Double sellingPrice;
}
