package com.increff.pos.api;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.model.exception.ApiException;

import java.util.List;
import java.util.Map;

public interface InventoryApi {
    void createInventoryIfAbsent(String productId) throws ApiException;
    InventoryPojo getByProductId(String productId) throws ApiException;
    InventoryPojo updateInventory(InventoryPojo inventoryPojo) throws ApiException;

    void incrementInventory(String productId, int delta) throws ApiException;
    void deductInventory(String productId, int quantity) throws ApiException;

    List<InventoryPojo> getByProductIds(List<String> ids);
    List<InventoryPojo> saveAll(List<InventoryPojo> list);

    boolean isSufficientInventoryBulk(Map<String, Integer> qtyByProductId) throws ApiException;
    void deductInventoryBulk(Map<String, Integer> quantityToDeductByProductId) throws ApiException;
    void incrementInventoryBulk(Map<String, Integer> quantityToAddByProductId) throws ApiException;
}
