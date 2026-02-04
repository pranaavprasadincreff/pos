package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryUpdatePojo {
    private String barcode;
    private Integer quantity;
}
