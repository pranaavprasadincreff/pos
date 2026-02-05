package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductBulkUploadDtoTest extends AbstractUnitTest {

    @Autowired
    private ProductDto productDto;

    @MockBean
    private ProductFlow productFlow;

    private static final String EMAIL_1 = "pranaav@increff.com";
    private static final String EMAIL_2 = "user@gmail.com";

    /* ===================== helpers ===================== */

    private static BulkUploadForm formFromTsv(String tsv) {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile(b64(tsv));
        return f;
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeB64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private static List<String[]> decodeResultRows(BulkUploadData data) {
        String decoded = decodeB64(data.file());
        decoded = decoded.replace("\r\n", "\n").replace("\r", "\n");

        String[] lines = decoded.split("\n", -1);
        List<String[]> out = new ArrayList<>();

        // skip header line
        for (int i = 1; i < lines.length; i++) {
            if (lines[i] == null || lines[i].isBlank()) continue;
            out.add(lines[i].split("\t", -1));
        }
        return out;
    }

    private static void assertRow(String[] row, String barcode, String status, String commentContainsOrNull) {
        assertEquals(barcode, row[0]);
        assertEquals(status, row[1]);

        if (commentContainsOrNull == null) {
            assertTrue(row[2] == null || row[2].isBlank(), "Expected blank comment, got: " + row[2]);
        } else {
            assertTrue(row[2].toLowerCase().contains(commentContainsOrNull.toLowerCase()),
                    "Expected comment containing [" + commentContainsOrNull + "] but got: " + row[2]);
        }
    }

    /* ===================== PRODUCTS ===================== */

    @Test
    public void bulkAddProducts_happyPath_twoRows() throws Exception {
        String tsv = ""
                + "barcode\tclientEmail\tname\tmrp\timageUrl\n"
                + "b001\t" + EMAIL_1 + "\tshirt\t499\thttp://img/1\n"
                + "B002\t" + EMAIL_2 + "\tpants\t799\t\n";

        // Return success for each incoming POJO (same order; skip nulls defensively)
        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);
                    List<String[]> res = new ArrayList<>();
                    for (ProductPojo p : pojos) {
                        if (p == null) {
                            res.add(new String[]{"", "ERROR", "Invalid row"});
                        } else {
                            res.add(new String[]{p.getBarcode(), "SUCCESS", ""});
                        }
                    }
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B001", "SUCCESS", null); // normalized to uppercase
        assertRow(rows.get(1), "B002", "SUCCESS", null);

        // Verify DTO -> Flow uses aligned list (same row count)
        ArgumentCaptor<List<ProductPojo>> cap = ArgumentCaptor.forClass(List.class);
        verify(productFlow, times(1)).bulkAddProducts(cap.capture());
        assertEquals(2, cap.getValue().size());
        assertEquals("B001", cap.getValue().get(0).getBarcode());
        assertEquals(EMAIL_1, cap.getValue().get(0).getClientEmail());
        assertEquals("shirt", cap.getValue().get(0).getName()); // normalized lower
        assertEquals(499.0, cap.getValue().get(0).getMrp());
    }

    @Test
    public void bulkAddProducts_missingRequiredHeader_failFast() {
        String tsv = ""
                + "barcode\tclientEmail\tname\n"
                + "B001\t" + EMAIL_1 + "\tshirt\n";

        ApiException ex = assertThrows(ApiException.class,
                () -> productDto.bulkAddProducts(formFromTsv(tsv)));

        assertTrue(ex.getMessage().toLowerCase().contains("missing required column"));
        verify(productFlow, never()).bulkAddProducts(anyList());
    }

    @Test
    public void bulkAddProducts_rowValidationError_mrpNotNumber_otherRowSuccess() throws Exception {
        String tsv = ""
                + "barcode\tclientEmail\tname\tmrp\n"
                + "B010\t" + EMAIL_1 + "\titem\tabc\n"
                + "B011\t" + EMAIL_1 + "\titem2\t100\n";

        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);

                    // DTO keeps alignment: 2 rows -> [null, validPojo]
                    assertEquals(2, pojos.size());
                    assertNull(pojos.get(0));
                    assertNotNull(pojos.get(1));
                    assertEquals("B011", pojos.get(1).getBarcode());

                    List<String[]> res = new ArrayList<>();
                    // flow returns aligned results too (one per input)
                    res.add(new String[]{"B010", "SUCCESS", ""}); // will be overridden by DTO error application
                    res.add(new String[]{"B011", "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B010", "ERROR", "mrp must be a number");
        assertRow(rows.get(1), "B011", "SUCCESS", null);
    }

    @Test
    public void bulkAddProducts_duplicateBarcodeInFile_bothRowsSentToFlow() throws Exception {
        // DTO does NOT reject duplicates in-file; Flow decides how to handle duplicates.
        String tsv = ""
                + "barcode\tclientEmail\tname\tmrp\n"
                + "B020\t" + EMAIL_1 + "\titem1\t100\n"
                + "B020\t" + EMAIL_1 + "\titem2\t200\n";

        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);
                    assertEquals(2, pojos.size());
                    assertNotNull(pojos.get(0));
                    assertNotNull(pojos.get(1));
                    assertEquals("B020", pojos.get(0).getBarcode());
                    assertEquals("B020", pojos.get(1).getBarcode());

                    // simulate flow rejecting second as duplicate
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{"B020", "SUCCESS", ""});
                    res.add(new String[]{"B020", "ERROR", "Duplicate barcode"});
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B020", "SUCCESS", null);
        assertRow(rows.get(1), "B020", "ERROR", "duplicate barcode");
    }

    @Test
    public void bulkAddProducts_businessErrorFromFlow_clientDoesNotExist() throws Exception {
        String tsv = ""
                + "barcode\tclientEmail\tname\tmrp\n"
                + "B030\t" + EMAIL_1 + "\titem\t100\n";

        List<String[]> flowRes = new ArrayList<>();
        flowRes.add(new String[]{"B030", "ERROR", "Client does not exist"});

        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenReturn(flowRes);

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(1, rows.size());
        assertRow(rows.get(0), "B030", "ERROR", "client does not exist");
    }

    /* ===================== INVENTORY ===================== */

    @Test
    public void bulkUpdateInventory_happyPath_twoRows() throws Exception {
        String tsv = ""
                + "barcode\tinventory\n"
                + "b001\t5\n"
                + "B002\t10\n";

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenAnswer(inv -> {
                    List<InventoryPojo> pojos = inv.getArgument(0);
                    List<String[]> res = new ArrayList<>();
                    for (InventoryPojo p : pojos) {
                        if (p == null) {
                            res.add(new String[]{"", "ERROR", "Invalid row"});
                        } else {
                            // barcode stored in productId for inventory bulk (your design)
                            res.add(new String[]{p.getProductId(), "SUCCESS", ""});
                        }
                    }
                    return res;
                });

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B001", "SUCCESS", null);
        assertRow(rows.get(1), "B002", "SUCCESS", null);

        ArgumentCaptor<List<InventoryPojo>> cap = ArgumentCaptor.forClass(List.class);
        verify(productFlow, times(1)).bulkUpdateInventory(cap.capture());
        assertEquals(2, cap.getValue().size());
        assertEquals("B001", cap.getValue().get(0).getProductId());
        assertEquals(5, cap.getValue().get(0).getQuantity());
    }

    @Test
    public void bulkUpdateInventory_missingRequiredHeader_failFast() {
        String tsv = ""
                + "barcode\tqty\n"
                + "B001\t5\n";

        ApiException ex = assertThrows(ApiException.class,
                () -> productDto.bulkUpdateInventory(formFromTsv(tsv)));

        assertTrue(ex.getMessage().toLowerCase().contains("required columns"));
        verify(productFlow, never()).bulkUpdateInventory(anyList());
    }

    @Test
    public void bulkUpdateInventory_rowValidationError_inventoryNotInt_otherRowSuccess() throws Exception {
        String tsv = ""
                + "barcode\tinventory\n"
                + "B040\tabc\n"
                + "B041\t7\n";

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenAnswer(inv -> {
                    List<InventoryPojo> pojos = inv.getArgument(0);

                    // DTO keeps alignment: 2 rows -> [null, validPojo]
                    assertEquals(2, pojos.size());
                    assertNull(pojos.get(0));
                    assertNotNull(pojos.get(1));
                    assertEquals("B041", pojos.get(1).getProductId());

                    List<String[]> res = new ArrayList<>();
                    // aligned response (row 0 will be overridden by DTO error)
                    res.add(new String[]{"B040", "SUCCESS", ""});
                    res.add(new String[]{"B041", "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B040", "ERROR", "valid integer");
        assertRow(rows.get(1), "B041", "SUCCESS", null);
    }

    @Test
    public void bulkUpdateInventory_businessError_productNotFound() throws Exception {
        String tsv = ""
                + "barcode\tinventory\n"
                + "NOPE1\t5\n";

        List<String[]> flowRes = new ArrayList<>();
        flowRes.add(new String[]{"NOPE1", "ERROR", "Product not found"});

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenReturn(flowRes);

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(1, rows.size());
        assertRow(rows.get(0), "NOPE1", "ERROR", "product not found");
    }

    @Test
    public void bulkUpdateInventory_duplicateBarcodeInFile_bothRowsSentToFlow_andBothApplied() throws Exception {
        // Per your requirement: do NOT remove duplicate barcodes; both positive rows should be applied.
        String tsv = ""
                + "barcode\tinventory\n"
                + "B050\t1\n"
                + "B050\t2\n";

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenAnswer(inv -> {
                    List<InventoryPojo> pojos = inv.getArgument(0);

                    // BOTH rows should reach flow
                    assertEquals(2, pojos.size());
                    assertNotNull(pojos.get(0));
                    assertNotNull(pojos.get(1));
                    assertEquals("B050", pojos.get(0).getProductId());
                    assertEquals(1, pojos.get(0).getQuantity());
                    assertEquals("B050", pojos.get(1).getProductId());
                    assertEquals(2, pojos.get(1).getQuantity());

                    // flow returns success for both rows (since it's allowed)
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{"B050", "SUCCESS", ""});
                    res.add(new String[]{"B050", "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B050", "SUCCESS", null);
        assertRow(rows.get(1), "B050", "SUCCESS", null);
    }
}
