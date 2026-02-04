package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class InventoryUpdateForm {

    @NotBlank(message = "Barcode is required")
    @Size(max = 40, message = "Barcode too long")
    private String barcode;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Inventory cannot be negative")
    @Max(value = 1000, message = "Inventory cannot exceed 1000")
    private Integer quantity;
}
