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

    private static final int INVENTORY_MAX = 1000;
    private static final String INSUFFICIENT_INVENTORY = "Insufficient inventory";

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
    public boolean isSufficientInventoryBulk(Map<String, Integer> qtyByProductId)
            throws ApiException {

        // TODO: remogve if not required
        validateBulkQuantityMap(qtyByProductId);

        List<String> productIds = qtyByProductId.keySet().stream().toList();
        List<InventoryPojo> inventories = inventoryDao.findByProductIds(productIds);

        Map<String, Integer> availableByProductId = new HashMap<>();
        for (InventoryPojo inv : inventories) {
            availableByProductId.put(inv.getProductId(), inv.getQuantity());
        }

        for (var entry : qtyByProductId.entrySet()) {
            int available = availableByProductId.getOrDefault(entry.getKey(), 0);
            if (available < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void deductInventoryBulk(Map<String, Integer> quantityByProductId)
            throws ApiException {

        validateBulkQuantityMap(quantityByProductId);
        inventoryDao.deductInventoryBulk(quantityByProductId);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventoryBulk(Map<String, Integer> quantityByProductId) throws ApiException {
        validateBulkQuantityMap(quantityByProductId);
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

    private int increaseQuantity(Integer currentQuantity, int delta) throws ApiException {
        int next = currentQuantity + delta;

        if (next > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }
        return next;
    }

    private void validateInventoryForBulkSave(InventoryPojo inventory) {
        if (inventory == null || inventory.getProductId() == null) {
            throw new RuntimeException("Invalid inventory input");
        }

        Integer quantity = inventory.getQuantity();
        if (quantity == null || quantity < 0) {
            throw new RuntimeException("Inventory cannot be negative");
        }

        if (quantity > INVENTORY_MAX) {
            throw new RuntimeException("Inventory cannot exceed " + INVENTORY_MAX);
        }
    }

    private void validateBulkQuantityMap(Map<String, Integer> quantityByProductId) throws ApiException {
        if (quantityByProductId == null || quantityByProductId.isEmpty()) {
            throw new ApiException("No inventory updates provided");
        }

        for (var entry : quantityByProductId.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (productId == null) {
                throw new ApiException("Invalid product id");
            }
            if (quantity == null || quantity <= 0) {
                throw new ApiException("Invalid quantity for product id: " + productId);
            }
        }
    }
}
