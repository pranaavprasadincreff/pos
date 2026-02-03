package com.increff.pos.model.data;

import com.increff.pos.model.constants.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthTokenData {
    private String email;
    private Role role;
    private String token;
}
