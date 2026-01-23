package com.increff.pos.model.form;

import lombok.Data;

@Data
public class OrderCreateItemForm {
    private String productBarcode;
    private Integer quantity;
    private Double sellingPrice;
}
