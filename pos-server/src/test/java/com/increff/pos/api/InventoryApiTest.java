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
    public void testUpdateInventoryNegativeShouldFail() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");

        InventoryPojo upd = new InventoryPojo();
        upd.setProductId("p1");
        upd.setQuantity(-1);

        assertThrows(ApiException.class, () -> inventoryApi.updateInventory(upd));
    }

    @Test
    public void testIncrementInventoryCapExceeded() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");
        assertThrows(ApiException.class, () -> inventoryApi.incrementInventory("p1", 1001));
    }

    @Test
    public void testDeductInventoryInsufficientShouldFail() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");
        assertThrows(ApiException.class, () -> inventoryApi.deductInventory("p1", 1));
    }

    @Test
    public void testDeductInventorySuccess() throws ApiException {
        inventoryApi.createInventoryIfAbsent("p1");
        inventoryApi.incrementInventory("p1", 10);

        assertDoesNotThrow(() -> inventoryApi.deductInventory("p1", 3));

        InventoryPojo inv = inventoryApi.getByProductId("p1");
        assertEquals(7, inv.getQuantity().intValue());
    }
}
