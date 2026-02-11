package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "products")
@CompoundIndexes({
        @CompoundIndex(name = "idx_products_barcode", def = "{'barcode': 1}", unique = true),
        @CompoundIndex(name = "idx_products_clientEmail_createdAt", def = "{'clientEmail': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_products_createdAt", def = "{'createdAt': -1}")
})
public class ProductPojo extends AbstractPojo {
    private String barcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}
