package com.increff.pos.db;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Data
@Document(collection = "users")
public class ClientUpdatePojo extends AbstractPojo {
    @Indexed(unique = true)
    private String oldEmail;
    private String newEmail;
    private String name;
}