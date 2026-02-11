package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "inventory")
@CompoundIndexes({
        @CompoundIndex(name = "idx_inventory_productId", def = "{'productId': 1}", unique = true)
})
public class InventoryPojo extends AbstractPojo {
    private String productId;
    private Integer quantity;
}
