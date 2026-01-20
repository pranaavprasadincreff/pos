package com.increff.pos.model.form;

import lombok.Data;

@Data
public class ProductUpdateForm {
    private String oldBarcode;
    private String newBarcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}