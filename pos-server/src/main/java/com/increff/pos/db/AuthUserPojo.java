package com.increff.pos.db;

import com.increff.pos.model.constants.Role;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "auth_users")
public class AuthUserPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String email;
    private String passwordHash;
    private Role role;
}
