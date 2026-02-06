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
    public void createInventoryIfAbsent(String productId) {
        InventoryPojo existingInventory = inventoryDao.findByProductId(productId);
        if (existingInventory != null) {
            return;
        }

        InventoryPojo inventoryToCreate = new InventoryPojo();
        inventoryToCreate.setProductId(productId);
        inventoryToCreate.setQuantity(0);
        inventoryDao.save(inventoryToCreate);
    }

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        return fetchInventoryByProductId(productId);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo inventoryUpdate) throws ApiException {
        validateInventoryUpdate(inventoryUpdate);

        InventoryPojo existingInventory = fetchInventoryByProductId(inventoryUpdate.getProductId());
        existingInventory.setQuantity(inventoryUpdate.getQuantity());

        return inventoryDao.save(existingInventory);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventory(String productId, int delta) throws ApiException {
        validateNonNegativeDelta(delta);

        InventoryPojo existingInventory = fetchInventoryByProductId(productId);
        int nextQuantity = calculateNextQuantity(existingInventory.getQuantity(), delta);

        existingInventory.setQuantity(nextQuantity);
        inventoryDao.save(existingInventory);
    }

    @Override
    public void deductInventory(String productId, int quantity) throws ApiException {
        boolean updated = inventoryDao.deductInventoryAtomically(productId, quantity);
        if (!updated) {
            throw new ApiException(INSUFFICIENT_INVENTORY);
        }
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
    public List<InventoryPojo> saveAll(List<InventoryPojo> inventoriesToSave) {
        if (inventoriesToSave == null || inventoriesToSave.isEmpty()) {
            return List.of();
        }

        for (InventoryPojo inventory : inventoriesToSave) {
            validateInventoryForBulkSave(inventory);
        }

        return inventoryDao.saveAll(inventoriesToSave);
    }

    @Override
    public boolean isSufficientInventoryBulk(Map<String, Integer> qtyByProductId)
            throws ApiException {

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

    private void validateNonNegativeDelta(int delta) throws ApiException {
        if (delta < 0) {
            throw new ApiException("Delta cannot be negative");
        }
    }

    private int calculateNextQuantity(Integer currentQuantity, int delta) throws ApiException {
        int current = currentQuantity == null ? 0 : currentQuantity;
        int next = current + delta;

        if (next > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }
        return next;
    }

    private void validateInventoryUpdate(InventoryPojo inventoryUpdate) throws ApiException {
        if (inventoryUpdate == null || inventoryUpdate.getProductId() == null) {
            throw new ApiException("Invalid inventory input");
        }

        Integer quantity = inventoryUpdate.getQuantity();
        if (quantity == null || quantity < 0) {
            throw new ApiException("Inventory cannot be negative");
        }

        if (quantity > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }
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
