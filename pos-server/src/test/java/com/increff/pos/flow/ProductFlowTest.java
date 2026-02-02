package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ProductFilterForm;
import com.increff.pos.test.AbstractUnitTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductFlowTest extends AbstractUnitTest {

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
        c.setName(name); // keep as provided; DTO layer normalizes, API/Flow need not
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
    public void testAddProductEnsuresClientExists() {
        ProductPojo p = product("p1", "missing@example.com");
        assertThrows(ApiException.class, () -> productFlow.addProduct(p));
    }

    @Test
    public void testAddProductCreatesInventoryIfAbsent() throws ApiException {
        createClient("c1@example.com", "client one");

        Pair<ProductPojo, InventoryPojo> pair = productFlow.addProduct(product("p1", "c1@example.com"));
        assertNotNull(pair);
        assertNotNull(pair.getLeft().getId());

        InventoryPojo inv = inventoryApi.getByProductId(pair.getLeft().getId());
        assertNotNull(inv);
        assertEquals(0, inv.getQuantity().intValue());
    }

    @Test
    public void testUpdateInventoryCapExceeded() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> pair = productFlow.addProduct(product("p1", "c1@example.com"));

        InventoryPojo invUpdate = new InventoryPojo();
        invUpdate.setProductId(pair.getLeft().getId());
        invUpdate.setQuantity(1001);

        assertThrows(ApiException.class, () -> productFlow.updateInventory(invUpdate));
    }

    @Test
    public void testGetAllAttachesInventory() throws ApiException {
        createClient("c1@example.com", "client one");
        productFlow.addProduct(product("p1", "c1@example.com"));
        productFlow.addProduct(product("p2", "c1@example.com"));

        PageForm pf = new PageForm();
        pf.setPage(0);
        pf.setSize(10);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.getAll(pf);
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 2);
        assertNotNull(page.getContent().get(0).getRight());
    }

    @Test
    public void testFilterByClientNameResolvesClientEmails() throws ApiException {
        // Distinct names so filtering can narrow to just alice
        createClient("alice@example.com", "alice");
        createClient("bob@example.com", "bob");

        productFlow.addProduct(product("p-alice", "alice@example.com"));
        productFlow.addProduct(product("p-bob", "bob@example.com"));

        ProductFilterForm ff = new ProductFilterForm();
        ff.setClient("ALICE");
        ff.setBarcode(null);
        ff.setName(null);
        ff.setPage(0);
        ff.setSize(10);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.filter(ff);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        // Flow/API preserves barcode casing as provided (DTO normalizes, Flow doesn't)
        assertEquals("p-alice", page.getContent().get(0).getLeft().getBarcode());
    }

    @Test
    public void testBulkUpdateInventoryAppliesDeltaAndCap() throws ApiException {
        createClient("c1@example.com", "client one");
        Pair<ProductPojo, InventoryPojo> p = productFlow.addProduct(product("p1", "c1@example.com"));

        InventoryPojo inc900 = new InventoryPojo();
        inc900.setProductId("p1"); // barcode carrier
        inc900.setQuantity(900);

        InventoryPojo inc200 = new InventoryPojo();
        inc200.setProductId("p1");
        inc200.setQuantity(200);

        List<String[]> results = productFlow.bulkUpdateInventory(List.of(inc900, inc200));
        assertEquals(2, results.size());

        assertEquals("SUCCESS", results.get(0)[1]);
        assertEquals("ERROR", results.get(1)[1]);
        assertTrue(results.get(1)[2].toLowerCase().contains("cannot exceed"));

        InventoryPojo inv = inventoryApi.getByProductId(p.getLeft().getId());
        assertEquals(900, inv.getQuantity().intValue());
    }
}
