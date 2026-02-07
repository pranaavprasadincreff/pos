package com.increff.pos.api;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class ProductApiTest extends AbstractUnitTest {

    @Autowired
    private ProductApi productApi;

    private ProductPojo product(String barcode) {
        ProductPojo p = new ProductPojo();
        p.setBarcode(barcode);
        p.setClientEmail("c@example.com");
        p.setName("n");
        p.setMrp(10.0);
        p.setImageUrl("x");
        return p;
    }

    @Test
    public void testAddDuplicateBarcodeShouldFail() throws ApiException {
        productApi.addProduct(product("dup"));
        assertThrows(ApiException.class, () -> productApi.addProduct(product("dup")));
    }

    @Test
    public void testGetByBarcodeNotFound() {
        assertThrows(ApiException.class, () -> productApi.getProductByBarcode("missing"));
    }

    @Test
    public void testUpdateToDuplicateBarcodeShouldFail() throws ApiException {
        productApi.addProduct(product("b1"));
        productApi.addProduct(product("b2"));

        ProductPojo update = product("b2");
        update.setClientEmail("c@example.com");
        update.setName("n");
        update.setMrp(10.0);
        update.setImageUrl("x");

        assertThrows(ApiException.class, () -> productApi.updateProduct(update, "b1"));
    }
}
