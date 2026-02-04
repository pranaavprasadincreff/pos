package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductData {
    private String barcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
    private Integer inventory;
}
