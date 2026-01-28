package com.increff.pos.api;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.model.exception.ApiException;

import java.util.List;

public interface InventoryApi {

    InventoryPojo addInventory(InventoryPojo inventoryPojo) throws ApiException;
    void createInventoryIfAbsent(String productId) throws ApiException;

    InventoryPojo getByProductId(String productId) throws ApiException;

    InventoryPojo updateInventory(InventoryPojo inventoryPojo) throws ApiException;

    void incrementInventory(InventoryPojo inventoryPojo, int delta) throws ApiException;

    void deductInventory(InventoryPojo inventory, int quantity) throws ApiException;

    boolean tryDeductInventoryForOrder(List<OrderItemPojo> items) throws ApiException;

    void restoreInventoryForOrder(List<OrderItemPojo> items) throws ApiException;
}
