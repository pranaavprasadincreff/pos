package com.increff.pos.model.form;

import lombok.Data;

@Data
public class ProductFilterForm {
    private String barcode;
    private String name;
    private String client;
    private int page = 0;
    private int size = 9;
}