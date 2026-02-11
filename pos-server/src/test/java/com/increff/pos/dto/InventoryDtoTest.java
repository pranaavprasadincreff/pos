package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryDtoTest extends AbstractUnitTest {

    private static final int INVENTORY_MAX = 1000;

    @Autowired
    private InventoryDto inventoryDto;

    @Autowired
    private ProductDto productDto;

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private InventoryApi inventoryApi;

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

    // ------------------------
    // SINGLE UPDATE INVENTORY
    // ------------------------

    @Test
    public void testUpdateInventoryProductNotFound() {
        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode("missing-barcode");
        inv.setQuantity(1);

        assertThrows(ApiException.class, () -> inventoryDto.updateInventory(inv));
    }

    @Test
    public void testUpdateInventoryCapExceededValidation() throws ApiException {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        InventoryUpdateForm inv = new InventoryUpdateForm();
        inv.setBarcode(created.getBarcode());
        inv.setQuantity(1001);

        ApiException e = assertThrows(ApiException.class, () -> inventoryDto.updateInventory(inv));
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

        ApiException e = assertThrows(ApiException.class, () -> inventoryDto.updateInventory(inv));
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

        ProductData updated = inventoryDto.updateInventory(inv);

        assertNotNull(updated);
        assertEquals("P1", updated.getBarcode());
        assertEquals(25, updated.getInventory().intValue());
    }

    // ------------------------
    // BULK UPDATE INVENTORY
    // ------------------------

    @Test
    public void testBulkUpdateInventoryBlankFileShouldFailValidation() {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile("");

        ApiException e = assertThrows(ApiException.class, () -> inventoryDto.bulkUpdateInventory(f));
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

        BulkUploadData out = inventoryDto.bulkUpdateInventory(f);
        assertNotNull(out);
        assertNotNull(out.file());

        String decodedLower = decodeB64(out.file()).toLowerCase();
        assertTrue(decodedLower.contains("error")
                        || decodedLower.contains("invalid")
                        || decodedLower.contains("quantity")
                        || decodedLower.contains("inventory"),
                "Expected output file to contain error/invalid but got:\n" + decodeB64(out.file()));
    }

    @Test
    public void testBulkUpdateInventoryExceedMaxClampsAndErrorsButPersists() throws Exception {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tinventory\n" +
                        "p1\t900\n" +
                        "p1\t200\n"
        ));

        BulkUploadData out = inventoryDto.bulkUpdateInventory(f);
        assertNotNull(out);
        String decoded = decodeB64(out.file()).toLowerCase();

        // both rows should be ERROR because clamping happened
        assertTrue(decoded.contains("error"), "Expected ERROR rows but got:\n" + decodeB64(out.file()));
        assertTrue(decoded.contains("clamped") || decoded.contains("cannot exceed"),
                "Expected clamp/exceed message but got:\n" + decodeB64(out.file()));

        // persisted inventory must be clamped to max
        ProductData fetched = productDto.getByBarcode(created.getBarcode());
        assertEquals(INVENTORY_MAX, fetched.getInventory().intValue());
    }

    @Test
    public void testBulkUpdateInventoryNegativeClampsToZeroAndErrorsButPersists() throws Exception {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tinventory\n" +
                        "p1\t-5\n"
        ));

        BulkUploadData out = inventoryDto.bulkUpdateInventory(f);
        assertNotNull(out);
        String decoded = decodeB64(out.file()).toLowerCase();

        assertTrue(decoded.contains("error"), "Expected ERROR rows but got:\n" + decodeB64(out.file()));
        assertTrue(decoded.contains("below 0") || decoded.contains("clamped"),
                "Expected below-0 clamp message but got:\n" + decodeB64(out.file()));

        ProductData fetched = productDto.getByBarcode(created.getBarcode());
        assertEquals(0, fetched.getInventory().intValue());
    }

    @Test
    public void testBulkUpdateInventorySuccessWhenWithinCap() throws Exception {
        createClient("c1@example.com", "Client One");
        ProductData created = productDto.addProduct(validProductForm("p1", "c1@example.com"));

        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(
                "barcode\tinventory\n" +
                        "p1\t400\n" +
                        "p1\t200\n"
        ));

        BulkUploadData out = inventoryDto.bulkUpdateInventory(f);
        assertNotNull(out);
        String decoded = decodeB64(out.file()).toLowerCase();

        assertTrue(decoded.contains("success"), "Expected SUCCESS rows but got:\n" + decodeB64(out.file()));

        ProductData fetched = productDto.getByBarcode(created.getBarcode());
        assertEquals(600, fetched.getInventory().intValue());
    }
}
