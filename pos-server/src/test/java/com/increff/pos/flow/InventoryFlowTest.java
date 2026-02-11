package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.test.AbstractUnitTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryFlowTest extends AbstractUnitTest {

    private static final int INVENTORY_MAX = 1000;

    @Autowired
    private InventoryFlow inventoryFlow;

    @Autowired
    private ProductFlow productFlow;

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private InventoryApi inventoryApi;

    private void createClient(String email, String name) throws ApiException {
        ClientPojo c = new ClientPojo();
        c.setEmail(email.trim().toLowerCase());
        c.setName(name);
        clientApi.add(c);
    }

    private ProductPojo product(String barcode, String clientEmail) {
        ProductPojo p = new ProductPojo();
        p.setBarcode(barcode);
        p.setClientEmail(clientEmail);
        p.setName("name");
        p.setMrp(10.0);
        p.setImageUrl("x");
        return p;
    }

    @Test
    public void testUpdateInventoryUpdatesQuantity() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> pair = productFlow.addProduct(product("p1", "c1@example.com"));

        InventoryPojo invUpdate = new InventoryPojo();
        invUpdate.setProductId(pair.getLeft().getId()); // productId (not barcode)
        invUpdate.setQuantity(25);

        Pair<ProductPojo, InventoryPojo> updated = inventoryFlow.updateInventory(invUpdate);
        assertNotNull(updated);
        assertEquals(25, updated.getRight().getQuantity().intValue());

        InventoryPojo inv = inventoryApi.getByProductId(pair.getLeft().getId());
        assertEquals(25, inv.getQuantity().intValue());
    }

    @Test
    public void testBulkUpdateInventoryAggregatesAndClampsToMaxButMarksError() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> created = productFlow.addProduct(product("p1", "c1@example.com"));

        InventoryPojo inc900 = new InventoryPojo();
        inc900.setProductId("p1"); // barcode carrier (bulk contract)
        inc900.setQuantity(900);

        InventoryPojo inc200 = new InventoryPojo();
        inc200.setProductId("p1"); // barcode carrier (bulk contract)
        inc200.setQuantity(200);

        List<String[]> results = inventoryFlow.bulkUpdateInventory(List.of(inc900, inc200));
        assertEquals(2, results.size());

        assertEquals("ERROR", results.get(0)[1]);
        assertEquals("ERROR", results.get(1)[1]);
        assertTrue(results.get(0)[2].toLowerCase().contains("clamped")
                || results.get(0)[2].toLowerCase().contains("cannot exceed"));

        InventoryPojo inv = inventoryApi.getByProductId(created.getLeft().getId());
        assertEquals(INVENTORY_MAX, inv.getQuantity().intValue());
    }

    @Test
    public void testBulkUpdateInventoryNegativeClampsToZeroButMarksError() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> created = productFlow.addProduct(product("p1", "c1@example.com"));

        // set current inventory to 10
        InventoryPojo seed = new InventoryPojo();
        seed.setProductId(created.getLeft().getId());
        seed.setQuantity(10);
        inventoryApi.updateInventory(seed);

        InventoryPojo dec15 = new InventoryPojo();
        dec15.setProductId("p1"); // barcode carrier
        dec15.setQuantity(-15);

        List<String[]> results = inventoryFlow.bulkUpdateInventory(List.of(dec15));
        assertEquals(1, results.size());

        assertEquals("ERROR", results.get(0)[1]);
        assertTrue(results.get(0)[2].toLowerCase().contains("below 0")
                || results.get(0)[2].toLowerCase().contains("clamped"));

        InventoryPojo inv = inventoryApi.getByProductId(created.getLeft().getId());
        assertEquals(0, inv.getQuantity().intValue());
    }

    @Test
    public void testBulkUpdateInventorySuccessWhenWithinCap() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> created = productFlow.addProduct(product("p1", "c1@example.com"));

        InventoryPojo inc400 = new InventoryPojo();
        inc400.setProductId("p1"); // barcode carrier (bulk contract)
        inc400.setQuantity(400);

        InventoryPojo inc200 = new InventoryPojo();
        inc200.setProductId("p1"); // barcode carrier (bulk contract)
        inc200.setQuantity(200);

        List<String[]> results = inventoryFlow.bulkUpdateInventory(List.of(inc400, inc200));
        assertEquals(2, results.size());

        assertEquals("SUCCESS", results.get(0)[1]);
        assertEquals("SUCCESS", results.get(1)[1]);

        InventoryPojo inv = inventoryApi.getByProductId(created.getLeft().getId());
        assertEquals(600, inv.getQuantity().intValue());
    }
}
