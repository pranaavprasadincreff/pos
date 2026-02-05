package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.BulkUploadData;
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

public class ProductBulkUploadDtoJumbledTest extends AbstractUnitTest {

    @Autowired
    private ProductDto productDto;

    @MockBean
    private ProductFlow productFlow;

    private static final String EMAIL_1 = "pranaav@increff.com";
    private static final String EMAIL_2 = "user@gmail.com";

    private static BulkUploadForm formFromTsv(String tsv) {
        BulkUploadForm f = new BulkUploadForm();
        f.setFile(Base64.getEncoder().encodeToString(tsv.getBytes(StandardCharsets.UTF_8)));
        return f;
    }

    private static String decodeB64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    @Test
    public void bulkAddProducts_jumbledColumns_mapsCorrectly() throws Exception {
        String tsv = ""
                + "mrp\tname\tbarcode\tclientEmail\timageUrl\n"
                + "499\tShirt\tb900\t" + EMAIL_1 + "\thttp://img/900\n"
                + "799\tPants\tB901\t" + EMAIL_2 + "\t\n";

        when(productFlow.bulkAddProducts(ArgumentMatchers.<ProductPojo>anyList()))
                .thenAnswer(inv -> {
                    List<ProductPojo> pojos = inv.getArgument(0);
                    List<String[]> res = new ArrayList<>();
                    for (ProductPojo p : pojos) {
                        if (p == null) res.add(new String[]{"", "ERROR", "Invalid row"});
                        else res.add(new String[]{p.getBarcode(), "SUCCESS", ""});
                    }
                    return res;
                });

        BulkUploadData out = productDto.bulkAddProducts(formFromTsv(tsv));
        assertNotNull(out);
        assertNotNull(out.file());
        assertTrue(decodeB64(out.file()).contains("barcode\tstatus\tcomment"));

        ArgumentCaptor<List<ProductPojo>> cap = ArgumentCaptor.forClass(List.class);
        verify(productFlow, times(1)).bulkAddProducts(cap.capture());

        List<ProductPojo> sent = cap.getValue();
        assertEquals(2, sent.size());

        ProductPojo p0 = sent.get(0);
        assertEquals("B900", p0.getBarcode());          // normalized
        assertEquals(EMAIL_1, p0.getClientEmail());
        assertEquals("shirt", p0.getName());            // normalized lower
        assertEquals(499.0, p0.getMrp());
        assertEquals("http://img/900", p0.getImageUrl());

        ProductPojo p1 = sent.get(1);
        assertEquals("B901", p1.getBarcode());
        assertEquals(EMAIL_2, p1.getClientEmail());
        assertEquals("pants", p1.getName());
        assertEquals(799.0, p1.getMrp());
    }

    @Test
    public void bulkUpdateInventory_jumbledColumns_mapsCorrectly() throws Exception {
        String tsv = ""
                + "inventory\tbarcode\n"
                + "5\tb910\n"
                + "10\tB911\n";

        when(productFlow.bulkUpdateInventory(ArgumentMatchers.<InventoryPojo>anyList()))
                .thenAnswer(inv -> {
                    List<InventoryPojo> pojos = inv.getArgument(0);
                    List<String[]> res = new ArrayList<>();
                    for (InventoryPojo p : pojos) {
                        if (p == null) res.add(new String[]{"", "ERROR", "Invalid row"});
                        else res.add(new String[]{p.getProductId(), "SUCCESS", ""});
                    }
                    return res;
                });

        BulkUploadData out = productDto.bulkUpdateInventory(formFromTsv(tsv));
        assertNotNull(out);

        ArgumentCaptor<List<InventoryPojo>> cap = ArgumentCaptor.forClass(List.class);
        verify(productFlow, times(1)).bulkUpdateInventory(cap.capture());

        List<InventoryPojo> sent = cap.getValue();
        assertEquals(2, sent.size());

        InventoryPojo i0 = sent.get(0);
        assertEquals("B910", i0.getProductId()); // barcode stored in productId
        assertEquals(5, i0.getQuantity());

        InventoryPojo i1 = sent.get(1);
        assertEquals("B911", i1.getProductId());
        assertEquals(10, i1.getQuantity());
    }
}
