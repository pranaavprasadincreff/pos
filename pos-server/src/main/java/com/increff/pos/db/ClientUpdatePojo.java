package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Getter
@Setter
@Document(collection = "users")
public class ClientUpdatePojo extends AbstractPojo {
    @Indexed(unique = true)
    private String oldEmail;
    private String newEmail;
    private String name;
}