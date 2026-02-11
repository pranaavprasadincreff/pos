package com.increff.pos.db;

import com.increff.pos.model.constants.Role;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "auth_users")
@CompoundIndex(name = "idx_auth_users_createdAt", def = "{'createdAt': -1}")
@CompoundIndex(name = "idx_auth_users_email", def = "{'email': 1}", unique = true)
public class AuthUserPojo extends AbstractPojo {

    private String email;
    private String passwordHash;
    private Role role;
}

