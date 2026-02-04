package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "products")
public class ProductPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String barcode;

    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}
