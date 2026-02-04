package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "inventory")
public class InventoryPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String productId;

    private Integer quantity;
}
