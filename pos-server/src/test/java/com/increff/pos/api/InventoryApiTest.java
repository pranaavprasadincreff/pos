package com.increff.pos.api;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryApiTest extends AbstractUnitTest {

    @Autowired
    private InventoryApi inventoryApi;

    @Test
    public void testCreateInventoryIfAbsentCreatesOnce() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");
        inventoryApi.createInventoryIfAbsent("p1"); // no duplicate creation

        InventoryPojo inv = inventoryApi.getByProductId("p1");
        assertNotNull(inv);
        assertEquals(0, inv.getQuantity().intValue());
    }

    @Test
    public void testUpdateInventoryPersistsQuantity() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");

        InventoryPojo upd = new InventoryPojo();
        upd.setProductId("p1");
        upd.setQuantity(10);

        InventoryPojo updated = inventoryApi.updateInventory(upd);
        assertNotNull(updated);
        assertEquals(10, updated.getQuantity().intValue());

        InventoryPojo fetched = inventoryApi.getByProductId("p1");
        assertEquals(10, fetched.getQuantity().intValue());
    }

    @Test
    public void testUpdateInventoryMissingProductShouldFail() {
        InventoryPojo upd = new InventoryPojo();
        upd.setProductId("missing");
        upd.setQuantity(10);

        assertThrows(ApiException.class, () -> inventoryApi.updateInventory(upd));
    }
}
