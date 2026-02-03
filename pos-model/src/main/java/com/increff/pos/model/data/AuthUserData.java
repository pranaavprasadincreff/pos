package com.increff.pos.model.data;

import com.increff.pos.model.constants.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthUserData {
    private String email;
    private Role role;
}
