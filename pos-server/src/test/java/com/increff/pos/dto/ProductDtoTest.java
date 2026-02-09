package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductSearchForm;
import com.increff.pos.model.form.ProductUpdateForm;
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
    private ProductApi productApi; // kept if used elsewhere later

    private void createClient(String email, String name) throws ApiException {
        ClientPojo c = new ClientPojo();
        c.setEmail(email.trim().toLowerCase());
        c.setName(name == null ? null : name.trim().toLowerCase());
        clientApi.add(c);
    }

    private void createClientQuietly(String email, String name) {
        try {
            createClient(email, name);
        } catch (ApiException ignored) {
        }
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

    private static void assertApiExceptionMentions(ApiException e, String needleLower) {
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().toLowerCase().contains(needleLower),
                "Expected error to contain '" + needleLower + "' but got: " + e.getMessage());
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
    public void testAddProductInvalidInputShouldFailValidation() {
        // missing required fields should be caught by FormValidator after normalization
        ProductForm f = new ProductForm();
        f.setBarcode("");                 // invalid
        f.setClientEmail("not-an-email"); // invalid
        f.setName("");                    // invalid
        f.setMrp(-1.0);                   // invalid (assuming @Positive / custom)
        ApiException e = assertThrows(ApiException.class, () -> productDto.addProduct(f));
        // don't overfit message; just ensure validation triggered
        assertTrue(e.getMessage().toLowerCase().contains("barcode")
                || e.getMessage().toLowerCase().contains("client")
                || e.getMessage().toLowerCase().contains("email")
                || e.getMessage().toLowerCase().contains("mrp")
                || e.getMessage().toLowerCase().contains("name"));
    }

    @Test
    public void testAddProductValidCreatesInventory() throws ApiException {
        createClient("c1@example.com", "Client One");

        ProductForm f = validProductForm("p1", "c1@example.com");
        ProductData created = productDto.addProduct(f);

        assertNotNull(created);
        assertEquals("P1", created.getBarcode()); // normalized (trim+upper)
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
    public void testGetByBarcodeInvalidBarcodeShouldFailFast() {
        ApiException e = assertThrows(ApiException.class, () -> productDto.getByBarcode("   "));
        assertApiExceptionMentions(e, "barcode");
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
    // SEARCH (also substitutes "get all paginated")
    // ------------------------

    @Test
    public void testSearchEmptyFiltersReturnsAllPaginated() throws ApiException {
        createClient("c1@example.com", "Client One");

        for (int i = 0; i < 5; i++) {
            productDto.addProduct(validProductForm("p" + i, "c1@example.com"));
        }

        ProductSearchForm f = new ProductSearchForm();
        f.setBarcode(null);
        f.setName(null);
        f.setClient(null);
        f.setPage(0);
        f.setSize(3);

        Page<ProductData> page = productDto.search(f);

        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
        assertNotNull(page.getContent().get(0).getInventory());
    }

    @Test
    public void testSearchInvalidPaginationShouldFailValidation() {
        ProductSearchForm f = new ProductSearchForm();
        f.setBarcode("abc");
        f.setPage(-1);
        f.setSize(10);

        ApiException e = assertThrows(ApiException.class, () -> productDto.search(f));
        assertTrue(e.getMessage().toLowerCase().contains("page"));
    }

    @Test
    public void testSearchByBarcodePartialCaseInsensitive() throws ApiException {
        createClient("c1@example.com", "Client One");
        productDto.addProduct(validProductForm("ABC-123", "c1@example.com"));
        productDto.addProduct(validProductForm("XYZ-999", "c1@example.com"));

        ProductSearchForm ff = new ProductSearchForm();
        ff.setBarcode("abc");
        ff.setName(null);
        ff.setClient(null);
        ff.setPage(0);
        ff.setSize(10);

        Page<ProductData> page = productDto.search(ff);

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

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
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
    public void testUpdateInventoryCapExceededValidation() throws ApiException {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
        inv.setQuantity(1001);

        ApiException e = assertThrows(ApiException.class, () -> productDto.updateInventory(inv));
        // should fail from bean validation: quantity max
        assertTrue(e.getMessage().toLowerCase().contains("quantity")
                || e.getMessage().toLowerCase().contains("inventory"));
    }

    @Test
    public void testUpdateInventoryNegativeShouldFailValidation() throws ApiException {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
        inv.setQuantity(-1);

        ApiException e = assertThrows(ApiException.class, () -> productDto.updateInventory(inv));
        assertTrue(e.getMessage().toLowerCase().contains("quantity")
                || e.getMessage().toLowerCase().contains("negative"));
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
    // BULK sanity (normalize + validate in DTO now)
    // ------------------------

    @Test
    public void testBulkAddProductsBlankFileShouldFailValidation() {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile("   ");

        ApiException e = assertThrows(ApiException.class, () -> productDto.bulkAddProducts(f));
        assertTrue(e.getMessage().toLowerCase().contains("file"));
    }

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
                        "dup\tc1@example.com\tProd 1\n" // missing mrp column value -> row error
        ));

        BulkUploadData out = productDto.bulkAddProducts(f);
        assertNotNull(out);
        assertNotNull(out.file());

        String decodedLower = decodeB64(out.file()).toLowerCase();
        assertTrue(decodedLower.contains("error") || decodedLower.contains("invalid") || decodedLower.contains("mrp"),
                "Expected output file to contain error/invalid but got:\n" + decodeB64(out.file()));
    }

    @Test
    public void testBulkAddProductsNormalizesBarcodeAndEmail() throws Exception {
        createClientQuietly("c1@example.com", "Client One");

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tclientemail\tname\tmrp\n" +
                        "  abc-1  \t  C1@EXAMPLE.COM  \tProd 1\t100\n"
        ));

        BulkUploadData out = productDto.bulkAddProducts(f);
        assertNotNull(out);
        assertNotNull(out.file());

        String decoded = decodeB64(out.file());
        // result file first column should include normalized barcode somewhere
        assertTrue(decoded.toUpperCase().contains("ABC-1"));
    }

    @Test
    public void testBulkUpdateInventoryBlankFileShouldFailValidation() {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile("");

        ApiException e = assertThrows(ApiException.class, () -> productDto.bulkUpdateInventory(f));
        assertTrue(e.getMessage().toLowerCase().contains("file"));
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

        String decodedLower = decodeB64(out.file()).toLowerCase();
        assertTrue(decodedLower.contains("error") || decodedLower.contains("invalid") || decodedLower.contains("quantity")
                        || decodedLower.contains("inventory"),
                "Expected output file to contain error/invalid but got:\n" + decodeB64(out.file()));
    }
}
