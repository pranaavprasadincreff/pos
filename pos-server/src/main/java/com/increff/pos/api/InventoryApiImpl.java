package com.increff.pos.api;

import com.increff.pos.dao.InventoryDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class InventoryApiImpl implements InventoryApi {
    private static final Logger logger = LoggerFactory.getLogger(InventoryApiImpl.class);
    private final InventoryDao dao;

    public InventoryApiImpl(InventoryDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo addInventory(InventoryPojo inventory) throws ApiException {
        logger.info("Creating inventory for productId: {}", inventory.getProductId());
        validateNewInventory(inventory);
        return dao.save(inventory);
    }

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        InventoryPojo inventory = dao.findByProductId(productId);
        if (inventory == null) {
            throw new ApiException("Inventory not found for productId: " + productId);
        }
        return inventory;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo inventory) throws ApiException {
        logger.info("Updating inventory for productId: {}", inventory.getProductId());
        InventoryPojo existing = getByProductId(inventory.getProductId());
        validateQuantity(inventory.getQuantity());
        existing.setQuantity(inventory.getQuantity());
        return dao.save(existing);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventory(InventoryPojo inventory, int delta) throws ApiException {
        logger.info("Incrementing inventory for productId: {} by {}", inventory.getProductId(), delta);
        validateDelta(delta);
        InventoryPojo existing = getByProductId(inventory.getProductId());
        existing.setQuantity(existing.getQuantity() + delta);
        dao.save(existing);
    }

    private void validateNewInventory(InventoryPojo inventory) throws ApiException {
        ensureInventoryDoesNotExist(inventory.getProductId());
        validateQuantity(inventory.getQuantity());
    }

    private void validateQuantity(Integer quantity) throws ApiException {
        if (quantity == null || quantity < 0) {
            throw new ApiException("Inventory quantity cannot be negative");
        }
    }

    private void validateDelta(int delta) throws ApiException {
        if (delta < 0) {
            throw new ApiException("Quantity increment cannot be negative");
        }
    }

    private void ensureInventoryDoesNotExist(String productId) throws ApiException {
        if (dao.findByProductId(productId) != null) {
            throw new ApiException("Inventory already exists for productId: " + productId);
        }
    }
}