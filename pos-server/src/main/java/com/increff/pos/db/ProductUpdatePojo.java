package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class ProductUpdatePojo extends AbstractPojo {
    @Indexed(unique = true)
    private String oldBarcode;

    private String newBarcode;
    private String clientEmail;
    private String name;
    private Double mrp;
    private String imageUrl;
}
