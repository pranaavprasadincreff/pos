package com.increff.pos.api;

import com.increff.pos.dao.InventoryDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryApiImpl implements InventoryApi {
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

        if (input == null || input.getProductId() == null) {
            throw new ApiException("Invalid inventory input");
        }
        if (input.getQuantity() < 0) {
            throw new ApiException("Inventory cannot be negative");
        }

        InventoryPojo existing = getByProductId(input.getProductId());
        existing.setQuantity(input.getQuantity());
        return dao.save(existing);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void incrementInventory(String productId, int delta) throws ApiException {
        InventoryPojo inv = getByProductId(productId);
        inv.setQuantity(inv.getQuantity() + delta);
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
    public List<InventoryPojo> saveAll(List<InventoryPojo> list) {
        return dao.saveAll(list);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public boolean tryDeductInventoryForOrder(List<OrderItemPojo> items) throws ApiException {
        // Check if all items have enough inventory
        for (OrderItemPojo item : items) {
            InventoryPojo inv = getByProductId(item.getProductBarcode());
            if (inv.getQuantity() < item.getOrderedQuantity()) {
                return false; // cannot fulfill
            }
        }

        // Deduct inventory
        for (OrderItemPojo item : items) {
            deductInventory(item.getProductBarcode(), item.getOrderedQuantity());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void restoreInventoryForOrder(List<OrderItemPojo> items) throws ApiException {
        for (OrderItemPojo item : items) {
            incrementInventory(item.getProductBarcode(), item.getOrderedQuantity());
        }
    }
}
