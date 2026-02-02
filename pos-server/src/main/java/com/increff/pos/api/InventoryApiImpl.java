package com.increff.pos.api;

import com.increff.pos.dao.InventoryDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryApiImpl implements InventoryApi {

    private static final int INVENTORY_MAX = 1000;

    @Autowired
    private InventoryDao dao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void createInventoryIfAbsent(String productId) {
        if (dao.findByProductId(productId) != null) return;

        InventoryPojo inv = new InventoryPojo();
        inv.setProductId(productId);
        inv.setQuantity(0);
        dao.save(inv);
    }

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        InventoryPojo inv = dao.findByProductId(productId);
        if (inv == null) {
            throw new ApiException("Inventory not found for productId: " + productId);
        }
        return inv;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo input) throws ApiException {
        validateUpdateInput(input);

        InventoryPojo existing = getByProductId(input.getProductId());
        existing.setQuantity(input.getQuantity());

        return dao.save(existing);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventory(String productId, int delta) throws ApiException {
        if (delta < 0) throw new ApiException("Delta cannot be negative");

        InventoryPojo inv = getByProductId(productId);
        int current = inv.getQuantity() == null ? 0 : inv.getQuantity();
        int next = current + delta;

        if (next > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }

        inv.setQuantity(next);
        dao.save(inv);
    }

    @Override
    public void deductInventory(String productId, int quantity) throws ApiException {
        boolean success = dao.deductInventoryAtomically(productId, quantity);
        if (!success) {
            throw new ApiException("Insufficient inventory");
        }
    }

    @Override
    public List<InventoryPojo> getByProductIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return dao.findByProductIds(ids);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public List<InventoryPojo> saveAll(List<InventoryPojo> list) {
        if (list == null || list.isEmpty()) return List.of();
        for (InventoryPojo p : list) {
            if (p == null || p.getProductId() == null) {
                throw new RuntimeException("Invalid inventory input");
            }
            if (p.getQuantity() == null || p.getQuantity() < 0) {
                throw new RuntimeException("Inventory cannot be negative");
            }
            if (p.getQuantity() > INVENTORY_MAX) {
                throw new RuntimeException("Inventory cannot exceed " + INVENTORY_MAX);
            }
        }
        return dao.saveAll(list);
    }

    private void validateUpdateInput(InventoryPojo input) throws ApiException {
        if (input == null || input.getProductId() == null) {
            throw new ApiException("Invalid inventory input");
        }
        if (input.getQuantity() == null || input.getQuantity() < 0) {
            throw new ApiException("Inventory cannot be negative");
        }
        if (input.getQuantity() > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }
    }
}
