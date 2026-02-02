package com.increff.pos.db;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Data
@Document(collection = "clients")
public class ClientPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String email;
    private String name;
} 