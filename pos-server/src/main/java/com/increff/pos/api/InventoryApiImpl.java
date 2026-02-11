package com.increff.pos.api;

import com.increff.pos.dao.InventoryDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryApiImpl implements InventoryApi {

    @Autowired
    private InventoryDao inventoryDao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo createInventoryIfAbsent(String productId) {
        InventoryPojo existingInventory = inventoryDao.findByProductId(productId);
        if (existingInventory != null) {
            return null;
        }

        InventoryPojo inventoryToCreate = new InventoryPojo();
        inventoryToCreate.setProductId(productId);
        inventoryToCreate.setQuantity(0);
        return inventoryDao.save(inventoryToCreate);
    }

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        return fetchInventoryByProductId(productId);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo inventoryUpdate) throws ApiException {
        InventoryPojo existingInventory = fetchInventoryByProductId(inventoryUpdate.getProductId());
        existingInventory.setQuantity(inventoryUpdate.getQuantity());
        return inventoryDao.save(existingInventory);
    }

    @Override
    public List<InventoryPojo> getByProductIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return inventoryDao.findByProductIds(ids);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void saveAll(List<InventoryPojo> inventoriesToSave) {
        inventoryDao.saveAll(inventoriesToSave);
    }

    @Override
    public boolean isSufficientInventoryBulk(Map<String, Integer> requiredQuantityByProductId)
            throws ApiException {

        if (requiredQuantityByProductId == null || requiredQuantityByProductId.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Integer> entry : requiredQuantityByProductId.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return false;
            }
        }

        List<String> productIds = requiredQuantityByProductId.keySet().stream().toList();
        List<InventoryPojo> inventories = inventoryDao.findByProductIds(productIds);

        Map<String, Integer> availableQuantityByProductId = new HashMap<>();
        for (InventoryPojo inv : inventories) {
            availableQuantityByProductId.put(inv.getProductId(), inv.getQuantity());
        }

        for (Map.Entry<String, Integer> entry : requiredQuantityByProductId.entrySet()) {
            String productId = entry.getKey();
            int requiredQty = entry.getValue();

            int availableQty = availableQuantityByProductId.getOrDefault(productId, 0);
            if (availableQty < requiredQty) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void deductInventoryBulk(Map<String, Integer> quantityByProductId) throws ApiException {
        inventoryDao.deductInventoryBulk(quantityByProductId);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventoryBulk(Map<String, Integer> quantityByProductId) throws ApiException {
        inventoryDao.incrementInventoryBulk(quantityByProductId);
    }

    // -------------------- private helpers --------------------

    private InventoryPojo fetchInventoryByProductId(String productId) throws ApiException {
        InventoryPojo inventory = inventoryDao.findByProductId(productId);
        if (inventory == null) {
            throw new ApiException("Inventory not found for productId: " + productId);
        }
        return inventory;
    }
}
