package com.increff.pos.model.form;

import lombok.Data;
import lombok.NonNull;

//Todo use getter/setter
@Data
public class OrderCreateItemForm {
    @NonNull
    private String productBarcode;
    private Integer quantity;
    private Double sellingPrice;
}
