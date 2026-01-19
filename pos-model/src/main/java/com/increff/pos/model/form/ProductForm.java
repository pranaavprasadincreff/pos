package com.increff.pos.model.form;

import lombok.Data;

@Data
public class ProductForm {
    private String barcode;
    private String clientId;
    private String name;
    private Double mrp;
    private String imageUrl;
}
