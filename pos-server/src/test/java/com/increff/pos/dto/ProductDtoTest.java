package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDtoTest extends AbstractUnitTest {

    @Autowired
    private ProductDto productDto;

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private ProductApi productApi;

    private void createClient(String email, String name) throws ApiException {
        ClientPojo c = new ClientPojo();
        c.setEmail(email.trim().toLowerCase());
        c.setName(name == null ? null : name.trim().toLowerCase());
        clientApi.add(c);
    }

    private ProductForm validProductForm(String barcode, String clientEmail) {
        ProductForm f = new ProductForm();
        f.setBarcode(barcode);
        f.setClientEmail(clientEmail);
        f.setName("Test Product");
        f.setMrp(100.0);
        f.setImageUrl("http://example.com/img.png");
        return f;
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeB64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private void createClientQuietly(String email, String name) {
        try {
            createClient(email, name);
        } catch (ApiException ignored) {
        }
    }

    // ------------------------
    // ADD / GET
    // ------------------------

    @Test
    public void testAddProductClientDoesNotExist() {
        ProductForm f = validProductForm("p1", "missing@example.com");
        assertThrows(ApiException.class, () -> productDto.addProduct(f));
    }

    @Test
    public void testAddProductValidCreatesInventory() throws ApiException {
        createClient("c1@example.com", "Client One");

        ProductForm f = validProductForm("p1", "c1@example.com");
        ProductData created = productDto.addProduct(f);

        assertNotNull(created);
        assertEquals("P1", created.getBarcode()); // normalized
        assertEquals("c1@example.com", created.getClientEmail());
        assertEquals(100.0, created.getMrp());
        assertNotNull(created.getInventory());
        assertEquals(0, created.getInventory().intValue());
        assertEquals("test product", created.getName().toLowerCase());
    }

    @Test
    public void testAddProductDuplicateBarcodeShouldFail() throws ApiException {
        createClient("c1@example.com", "Client One");

        productDto.addProduct(validProductForm("dup", "c1@example.com"));
        assertThrows(ApiException.class, () -> productDto.addProduct(validProductForm("DUP", "c1@example.com")));
    }

    @Test
    public void testGetByBarcodeNotFound() {
        assertThrows(ApiException.class, () -> productDto.getByBarcode("missing"));
    }

    @Test
    public void testGetByBarcodeSuccess() throws ApiException {
        createClient("c1@example.com", "Client One");

        productDto.addProduct(validProductForm("p-get", "c1@example.com"));
        ProductData fetched = productDto.getByBarcode("  P-GET  ");

        assertNotNull(fetched);
        assertEquals("P-GET", fetched.getBarcode());
        assertNotNull(fetched.getInventory());
        assertEquals(0, fetched.getInventory().intValue());
    }

    // ------------------------
    // PAGINATION / FILTER
    // ------------------------

    @Test
    public void testGetAllInvalidPageSize() {
        PageForm pf = new PageForm();
        pf.setPage(0);
        pf.setSize(101);
        assertThrows(ApiException.class, () -> productDto.getAll(pf));
    }

    @Test
    public void testGetAllValid() throws ApiException {
        createClient("c1@example.com", "Client One");

        for (int i = 0; i < 5; i++) {
            productDto.addProduct(validProductForm("p" + i, "c1@example.com"));
        }

        PageForm pf = new PageForm();
        pf.setPage(0);
        pf.setSize(3);

        Page<ProductData> page = productDto.getAll(pf);

        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
        assertNotNull(page.getContent().get(0).getInventory());
    }

    @Test
    public void testFilterByBarcodePartialCaseInsensitive() throws ApiException {
        createClient("c1@example.com", "Client One");
        productDto.addProduct(validProductForm("ABC-123", "c1@example.com"));
        productDto.addProduct(validProductForm("XYZ-999", "c1@example.com"));

        ProductFilterForm ff = new ProductFilterForm();
        ff.setBarcode("abc");
        ff.setName(null);
        ff.setClient(null);
        ff.setPage(0);
        ff.setSize(10);

        Page<ProductData> page = productDto.filter(ff);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("ABC-123", page.getContent().get(0).getBarcode());
    }

    // ------------------------
    // UPDATE PRODUCT
    // ------------------------

    @Test
    public void testUpdateProductNotFound() throws ApiException {
        createClient("c1@example.com", "Client One");

        ProductUpdateForm u = new ProductUpdateForm();
        u.setOldBarcode("missing");
        u.setNewBarcode("new");
        u.setClientEmail("c1@example.com");
        u.setName("Updated");
        u.setMrp(200.0);
        u.setImageUrl("http://example.com/u.png");

        assertThrows(ApiException.class, () -> productDto.updateProduct(u));
    }

    @Test
    public void testUpdateProductDuplicateNewBarcodeShouldFail() throws ApiException {
        createClient("c1@example.com", "Client One");

        productDto.addProduct(validProductForm("p1", "c1@example.com"));
        productDto.addProduct(validProductForm("p2", "c1@example.com"));

        ProductUpdateForm u = new ProductUpdateForm();
        u.setOldBarcode("p1");
        u.setNewBarcode("p2");
        u.setClientEmail("c1@example.com");
        u.setName("Updated");
        u.setMrp(200.0);
        u.setImageUrl("http://example.com/u.png");

        assertThrows(ApiException.class, () -> productDto.updateProduct(u));
    }

    @Test
    public void testUpdateProductSuccessKeepsInventory() throws ApiException {
        createClient("c1@example.com", "Client One");

        ProductData created = productDto.addProduct(validProductForm("p-old", "c1@example.com"));

        // ✅ NEW: inventory update uses BARCODE (not productId)
        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode()); // already normalized
        inv.setQuantity(10);
        productDto.updateInventory(inv);

        ProductUpdateForm u = new ProductUpdateForm();
        u.setOldBarcode("p-old");
        u.setNewBarcode("p-new");
        u.setClientEmail("c1@example.com");
        u.setName("Updated Name");
        u.setMrp(250.0);
        u.setImageUrl("http://example.com/u.png");

        ProductData updated = productDto.updateProduct(u);

        assertNotNull(updated);
        assertEquals("P-NEW", updated.getBarcode());
        assertEquals(250.0, updated.getMrp());
        assertNotNull(updated.getInventory());
        assertEquals(10, updated.getInventory().intValue());
        assertEquals("updated name", updated.getName().toLowerCase());
    }

    // ------------------------
    // UPDATE INVENTORY (single)
    // ------------------------

    @Test
    public void testUpdateInventoryProductNotFound() {
        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode("missing-barcode");
        inv.setQuantity(1);

        assertThrows(ApiException.class, () -> productDto.updateInventory(inv));
    }

    @Test
    public void testUpdateInventoryCapExceeded() throws ApiException {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
        inv.setQuantity(1001);

        assertThrows(ApiException.class, () -> productDto.updateInventory(inv));
    }

    @Test
    public void testUpdateInventorySuccess() throws ApiException {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
        inv.setQuantity(25);

        ProductData updated = productDto.updateInventory(inv);

        assertNotNull(updated);
        assertEquals("P1", updated.getBarcode());
        assertEquals(25, updated.getInventory().intValue());
    }

    // ------------------------
    // BULK sanity
    // ------------------------

    @Test
    public void testBulkAddProductsInvalidHeaders() {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64("badheader\tname\nx\ty\n"));
        assertThrows(ApiException.class, () -> productDto.bulkAddProducts(f));
    }

    @Test
    public void testBulkAddProductsInvalidTsvReturnsErrorFile() throws Exception {
        createClientQuietly("c1@example.com", "Client One");

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tclientemail\tname\tmrp\n" +
                        "dup\tc1@example.com\tProd 1\n"
        ));

        BulkUploadData out = productDto.bulkAddProducts(f);
        assertNotNull(out);
        assertNotNull(out.file());

        String decoded = decodeB64(out.file()).toLowerCase();
        assertTrue(decoded.contains("error") || decoded.contains("invalid"),
                "Expected output file to contain error/invalid but got:\n" + decodeB64(out.file()));
    }

    @Test
    public void testBulkUpdateInventoryInvalidTsvReturnsErrorFile() throws Exception {
        createClientQuietly("c1@example.com", "Client One");

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tinventory\n" +
                        "p1\n"
        ));

        BulkUploadData out = productDto.bulkUpdateInventory(f);
        assertNotNull(out);
        assertNotNull(out.file());

        String decoded = decodeB64(out.file()).toLowerCase();
        assertTrue(decoded.contains("error") || decoded.contains("invalid"),
                "Expected output file to contain error/invalid but got:\n" + decodeB64(out.file()));
    }
}
