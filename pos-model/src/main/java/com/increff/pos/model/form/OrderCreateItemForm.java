package com.increff.pos.model.form;

import lombok.*;

@Getter
@Setter
public class OrderCreateItemForm {
    private String productBarcode;
    private Integer quantity;
    private Double sellingPrice;
}
