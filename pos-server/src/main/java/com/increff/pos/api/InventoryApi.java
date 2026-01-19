package com.increff.pos.api;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.exception.ApiException;

public interface InventoryApi {
    InventoryPojo addInventory(InventoryPojo inventoryPojo) throws ApiException;
    InventoryPojo getByProductId(String productId) throws ApiException;
    InventoryPojo updateInventory(InventoryPojo inventoryPojo) throws ApiException;
}
