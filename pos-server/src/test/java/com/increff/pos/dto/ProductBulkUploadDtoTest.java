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

        // Return success for each incoming POJO (same order)
        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);
                    List<String[]> res = new ArrayList<>();
                    for (ProductPojo p : pojos) {
                        res.add(new String[]{p.getBarcode(), "SUCCESS", ""});
                    }
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B001", "SUCCESS", null); // normalized to uppercase
        assertRow(rows.get(1), "B002", "SUCCESS", null);

        // Verify DTO -> Flow uses POJOs only + normalized fields
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
                    // only one valid should reach flow
                    assertEquals(1, pojos.size());
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{pojos.get(0).getBarcode(), "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B010", "ERROR", "mrp must be a number");
        assertRow(rows.get(1), "B011", "SUCCESS", null);
    }

    @Test
    public void bulkAddProducts_duplicateBarcodeInFile_secondRowError() throws Exception {
        String tsv = ""
                + "barcode\tclientEmail\tname\tmrp\n"
                + "B020\t" + EMAIL_1 + "\titem1\t100\n"
                + "B020\t" + EMAIL_1 + "\titem2\t200\n";

        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);
                    // DTO should reject duplicate in file -> only one should reach flow
                    assertEquals(1, pojos.size());
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{pojos.get(0).getBarcode(), "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B020", "SUCCESS", null);
        assertRow(rows.get(1), "B020", "ERROR", "duplicate barcode in file");
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
                        // barcode stored in productId for inventory bulk (your design)
                        res.add(new String[]{p.getProductId(), "SUCCESS", ""});
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
                    // only one valid should reach flow
                    assertEquals(1, pojos.size());
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{pojos.get(0).getProductId(), "SUCCESS", ""});
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
    public void bulkUpdateInventory_duplicateBarcodeInFile_secondRowError() throws Exception {
        String tsv = ""
                + "barcode\tinventory\n"
                + "B050\t1\n"
                + "B050\t2\n";

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenAnswer(inv -> {
                    List<InventoryPojo> pojos = inv.getArgument(0);
                    // duplicate in file rejected by DTO
                    assertEquals(1, pojos.size());
                    List<String[]> res = new ArrayList<>();
                    res.add(new String[]{pojos.get(0).getProductId(), "SUCCESS", ""});
                    return res;
                });

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        List<String[]> rows = decodeResultRows(out);

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "B050", "SUCCESS", null);
        assertRow(rows.get(1), "B050", "ERROR", "duplicate barcode in file");
    }
}
