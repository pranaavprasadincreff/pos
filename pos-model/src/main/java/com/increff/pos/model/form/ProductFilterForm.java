package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ProductFilterForm {

    @Size(max = 40, message = "Barcode filter too long")
    private String barcode;

    @Size(max = 30, message = "Name filter too long")
    private String name;

    @Size(max = 40, message = "Client filter too long")
    private String client;

    @Min(value = 0, message = "Page cannot be negative")
    private int page = 0;

    @Min(value = 1, message = "Size must be >= 1")
    @Max(value = 100, message = "Size cannot exceed 100")
    private int size = 9;
}
