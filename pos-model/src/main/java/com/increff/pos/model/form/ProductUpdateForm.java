package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ProductUpdateForm {

    @NotBlank(message = "Old barcode is required")
    @Size(max = 40, message = "Old barcode too long")
    private String oldBarcode;

    @NotBlank(message = "New barcode is required")
    @Size(max = 40, message = "New barcode too long")
    private String newBarcode;

    @NotBlank(message = "Client email is required")
    @Size(max = 40, message = "Email too long")
    @Email(message = "Invalid email format")
    private String clientEmail;

    @NotBlank(message = "Product name is required")
    @Size(max = 30, message = "Name too long")
    private String name;

    @NotNull(message = "MRP is required")
    @Positive(message = "MRP must be > 0")
    private Double mrp;

    @Size(max = 500, message = "Image URL too long")
    private String imageUrl;
}
