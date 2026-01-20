package com.increff.pos.model.form;

import lombok.Data;

@Data
public class ProductForm {
    private String barcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}
