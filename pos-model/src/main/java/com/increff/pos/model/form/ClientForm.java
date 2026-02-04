package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ClientForm {

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name too long")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 40, message = "Email too long")
    private String email;
}
