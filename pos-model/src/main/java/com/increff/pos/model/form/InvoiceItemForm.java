package com.increff.pos.model.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvoiceItemForm {

    @NotBlank(message = "Barcode is required")
    @Size(max = 40, message = "Barcode too long")
    private String barcode;

    @NotBlank(message = "Product name is required")
    @Size(max = 30, message = "Product name too long")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Invalid quantity")
    @Max(value = 1000, message = "Quantity cannot exceed 1000")
    private Integer quantity;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Invalid selling price")
    private Double sellingPrice;
}
