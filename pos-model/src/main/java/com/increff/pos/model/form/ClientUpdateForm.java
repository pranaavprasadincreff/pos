package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ClientUpdateForm {

    @NotBlank(message = "Old email is required")
    @Email(message = "Invalid old email format")
    @Size(max = 40, message = "Old email too long")
    private String oldEmail;

    @NotBlank(message = "New email is required")
    @Email(message = "Invalid new email format")
    @Size(max = 40, message = "New email too long")
    private String newEmail;

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name too long")
    private String name;
}
