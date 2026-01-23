package com.increff.pos.model.form;

import lombok.Data;

@Data
public class InvoiceItemForm {
    private String barcode;
    private String productName;
    private Integer quantity;
    private Double sellingPrice;
}
