package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "inventory")
public class InventoryPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String productId;
    private Integer quantity;
}
