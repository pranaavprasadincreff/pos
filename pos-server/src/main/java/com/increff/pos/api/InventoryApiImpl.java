package com.increff.pos.api;

import com.increff.pos.dao.InventoryDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryApiImpl implements InventoryApi {

    private final InventoryDao dao;
    private final ProductApi productApi;

    public InventoryApiImpl(InventoryDao dao, ProductApi productApi) {
        this.dao = dao;
        this.productApi = productApi;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void createInventoryIfAbsent(String productId) throws ApiException {
        InventoryPojo existing = dao.findByProductId(productId);
        if (existing != null) {
            return;
        }

        InventoryPojo inventory = new InventoryPojo();
        inventory.setProductId(productId);
        inventory.setQuantity(0);
        dao.save(inventory);
    }

    // ---------- Phase 2 addition ----------

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void restoreInventoryForOrder(List<OrderItemPojo> items) throws ApiException {
        for (OrderItemPojo item : items) {
            InventoryPojo inventory = getInventoryForItem(item);
            inventory.setQuantity(
                    inventory.getQuantity() + item.getOrderedQuantity()
            );
            dao.save(inventory);
        }
    }

    // ---------- Phase 1 logic (unchanged) ----------

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public boolean tryDeductInventoryForOrder(List<OrderItemPojo> items) throws ApiException {
        if (!isOrderFulfillable(items)) {
            return false;
        }
        for (OrderItemPojo item : items) {
            InventoryPojo inventory = getInventoryForItem(item);
            deductInventory(inventory, item.getOrderedQuantity());
        }
        return true;
    }

    private boolean isOrderFulfillable(List<OrderItemPojo> items) throws ApiException {
        for (OrderItemPojo item : items) {
            InventoryPojo inventory = getInventoryForItem(item);
            if (inventory.getQuantity() < item.getOrderedQuantity()) {
                return false;
            }
        }
        return true;
    }

    private InventoryPojo getInventoryForItem(OrderItemPojo item) throws ApiException {
        String productId = productApi
                .getProductByBarcode(item.getProductBarcode())
                .getId();
        return getByProductId(productId);
    }

    // ---------- Existing APIs ----------

    @Override
    public InventoryPojo getByProductId(String productId) throws ApiException {
        InventoryPojo inventory = dao.findByProductId(productId);
        if (inventory == null) {
            throw new ApiException("Inventory not found for productId: " + productId);
        }
        return inventory;
    }

    @Override
    public void deductInventory(InventoryPojo inventory, int quantity) throws ApiException {
        boolean success = dao.deductInventoryAtomically(
                inventory.getProductId(),
                quantity
        );
        if (!success) {
            throw new ApiException("Insufficient inventory");
        }
    }

    @Override public InventoryPojo addInventory(InventoryPojo inventory) throws ApiException { return dao.save(inventory); }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public InventoryPojo updateInventory(InventoryPojo inventory) throws ApiException {
        if (inventory == null || inventory.getProductId() == null) {
            throw new ApiException("Invalid inventory input");
        }

        // Fetch existing inventory
        InventoryPojo existing = dao.findByProductId(inventory.getProductId());
        if (existing == null) {
            throw new ApiException("Inventory not found for productId: " + inventory.getProductId());
        }

        // Validate quantity
        if (inventory.getQuantity() < 0) {
            throw new ApiException("Inventory quantity cannot be negative");
        }

        existing.setQuantity(inventory.getQuantity());
        return dao.save(existing);
    }

    @Override public void incrementInventory(InventoryPojo inventory, int delta) throws ApiException {
        inventory.setQuantity(inventory.getQuantity() + delta);
        dao.save(inventory);
    }
}
