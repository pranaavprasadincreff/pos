package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
public class ProductUpdatePojo extends AbstractPojo{
    @Indexed(unique = true)
    private String oldBarcode;
    private String newBarcode;
    private String clientId;
    private String name;
    private Double mrp;
    private String imageUrl;
}
