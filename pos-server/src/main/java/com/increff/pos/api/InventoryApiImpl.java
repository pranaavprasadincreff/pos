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
    @Transactional
    public InventoryPojo addInventory(InventoryPojo inventoryPojo) throws ApiException {
        InventoryPojo existing = dao.findByProductId(inventoryPojo.getProductId());
        if (existing != null) {
            throw new ApiException("Inventory already exists for productId: " + inventoryPojo.getProductId());
        }
        return dao.save(inventoryPojo);
    }

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        InventoryPojo inventory = dao.findByProductId(productId);
        if (Objects.isNull(inventory)) {
            throw new ApiException(
                    "Inventory not found for productId: " + productId
            );
        }
        return inventory;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo inventoryPojo) throws ApiException {
        logger.info("Updating inventory for productId: {}",
                inventoryPojo.getProductId());
        InventoryPojo existing = getByProductId(inventoryPojo.getProductId());
        if (existing == null) {
            return dao.save(inventoryPojo);
        }
        if (inventoryPojo.getQuantity() < 0) {
            throw new ApiException("Inventory quantity cannot be negative");
        }
        existing.setQuantity(inventoryPojo.getQuantity());
        return dao.save(existing);
    }
}
