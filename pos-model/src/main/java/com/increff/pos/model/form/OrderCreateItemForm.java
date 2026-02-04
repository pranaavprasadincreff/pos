package com.increff.pos.model.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateItemForm {

    @NotBlank(message = "Product barcode cannot be empty")
    @Size(max = 40, message = "Barcode too long")
    private String productBarcode;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Invalid quantity")
    @Max(value = 1000, message = "Quantity cannot exceed 1000")
    private Integer quantity;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Invalid selling price")
    private Double sellingPrice;
}
