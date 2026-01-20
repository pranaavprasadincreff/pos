package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "products")
public class ProductPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String barcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}
