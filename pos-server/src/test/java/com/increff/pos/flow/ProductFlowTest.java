package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductSearchForm;
import com.increff.pos.test.AbstractUnitTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

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
    public void testFilterByClientNameResolvesClientEmails() throws ApiException {
        createClient("alice@example.com", "alice");
        createClient("bob@example.com", "bob");

        productFlow.addProduct(product("p-alice", "alice@example.com"));
        productFlow.addProduct(product("p-bob", "bob@example.com"));

        ProductSearchForm ff = new ProductSearchForm();
        ff.setClient("ALICE");
        ff.setBarcode(null);
        ff.setName(null);
        ff.setPage(0);
        ff.setSize(10);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.search(ff);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("p-alice", page.getContent().get(0).getLeft().getBarcode());
    }
}
