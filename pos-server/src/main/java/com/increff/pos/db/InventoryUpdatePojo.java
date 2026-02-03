package com.increff.pos.db;

import lombok.Data;

@Data
public class InventoryUpdatePojo {
    private String barcode;
    private Integer quantity;
}
