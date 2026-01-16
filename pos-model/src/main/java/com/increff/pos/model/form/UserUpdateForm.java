package com.increff.pos.model.form;

import lombok.Data;

@Data
public class UserUpdateForm {
    private String oldEmail;
    private String newEmail;
    private String name;
}
