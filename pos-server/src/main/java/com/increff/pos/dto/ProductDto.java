package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.InventoryUpdatePojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.helper.TsvHelper;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProductDto {
    @Autowired
    private ProductFlow productFlow;

    public ProductData addProduct(ProductForm form) throws ApiException {
        NormalizationUtil.normalizeProductForm(form);
        ValidationUtil.validateProductForm(form);

        ProductPojo pojo = ProductHelper.convertProductFormToEntity(form);
        // TODO: don't next function calls while returning
        return toData(productFlow.addProduct(pojo));
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        String normalized = NormalizationUtil.normalizeBarcode(barcode);
        return toData(productFlow.getByBarcode(normalized));
    }

    public Page<ProductData> getAll(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        return toDataPage(productFlow.getAll(form));
    }

    public Page<ProductData> filter(ProductFilterForm form) throws ApiException {
        NormalizationUtil.normalizeProductFilterForm(form);
        ValidationUtil.validateProductFilterForm(form);
        return toDataPage(productFlow.filter(form));
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeProductUpdateForm(form);
        ValidationUtil.validateProductUpdateForm(form);

        ProductUpdatePojo pojo = ProductHelper.convertProductUpdateFormToEntity(form);
        return toData(productFlow.updateProduct(pojo));
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeInventoryUpdateForm(form);
        ValidationUtil.validateInventoryUpdateForm(form);
        InventoryUpdatePojo pojo = ProductHelper.convertInventoryUpdateFormToEntity(form);
        return toData(productFlow.updateInventory(pojo));
    }


    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        ParsedBulkData parsed = parseAndValidateHeadersForProducts(form);
        RowProcessingResult<ProductPojo> processed = validateNormalizeAndConvertProducts(parsed);

        applyFlowResultsToRows(processed, productFlow.bulkAddProducts(processed.validPojos()));
        return encodeResult(processed.resultRows());
    }

    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        ParsedBulkData parsed = parseAndValidateHeadersForInventory(form);
        RowProcessingResult<InventoryPojo> processed = validateNormalizeAndConvertInventory(parsed);

        applyFlowResultsToRows(processed, productFlow.bulkUpdateInventory(processed.validPojos()));
        return encodeResult(processed.resultRows());
    }

    // -------------------- Private helpers --------------------

    // TODO: Shift to helper
    private ProductData toData(Pair<ProductPojo, InventoryPojo> pair) {
        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    private Page<ProductData> toDataPage(Page<Pair<ProductPojo, InventoryPojo>> page) {
        List<ProductData> data = page.getContent().stream().map(this::toData).toList();
        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    private ParsedBulkData parseAndValidateHeadersForProducts(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> data = TsvHelper.parse(form.getFile());
        ValidationUtil.validateBulkProductData(data.getLeft(), data.getRight());
        return new ParsedBulkData(data.getLeft(), data.getRight());
    }

    private ParsedBulkData parseAndValidateHeadersForInventory(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> data = TsvHelper.parse(form.getFile());
        ValidationUtil.validateBulkInventoryData(data.getLeft(), data.getRight());
        return new ParsedBulkData(data.getLeft(), data.getRight());
    }

    private RowProcessingResult<ProductPojo> validateNormalizeAndConvertProducts(ParsedBulkData parsed) {
        List<String[]> resultRows = new ArrayList<>();
        List<ProductPojo> validPojos = new ArrayList<>();
        Map<String, Integer> firstRowIndexByBarcode = new HashMap<>();
        Set<String> seenBarcodes = new HashSet<>();

        for (String[] rawRow : parsed.rows()) {
            String barcodeForOutput = cell(rawRow, parsed.headers(), "barcode");
            String[] out = errorResult(barcodeForOutput);
            int rowIndex = resultRows.size();
            resultRows.add(out);

            try {
                String[] canonical = toCanonicalProductRow(rawRow, parsed.headers());
                NormalizationUtil.normalizeBulkProductRows(Collections.singletonList(canonical));
                ValidationUtil.validateBulkProductRow(canonical);

                String normalizedBarcode = canonical[0];
                if (!seenBarcodes.add(normalizedBarcode)) {
                    throw new ApiException("Duplicate barcode in file");
                }

                ProductPojo pojo = ProductHelper.toBulkProductPojo(canonical);
                validPojos.add(pojo);
                //TODO: change name firstrowindexbybarcode
                firstRowIndexByBarcode.put(pojo.getBarcode(), rowIndex);
                resultRows.get(rowIndex)[0] = pojo.getBarcode();
            } catch (ApiException e) {
                resultRows.get(rowIndex)[2] = e.getMessage();
            }
        }
        return new RowProcessingResult<>(validPojos, resultRows, firstRowIndexByBarcode);
    }

    private RowProcessingResult<InventoryPojo> validateNormalizeAndConvertInventory(ParsedBulkData parsed) {
        List<String[]> resultRows = new ArrayList<>();
        List<InventoryPojo> validPojos = new ArrayList<>();
        Map<String, Integer> firstRowIndexByBarcode = new HashMap<>();
        Set<String> seenBarcodes = new HashSet<>();

        for (String[] rawRow : parsed.rows()) {
            String barcodeForOutput = cell(rawRow, parsed.headers(), "barcode");
            String[] out = errorResult(barcodeForOutput);
            int rowIndex = resultRows.size();
            resultRows.add(out);

            try {
                String[] canonical = toCanonicalInventoryRow(rawRow, parsed.headers());
                NormalizationUtil.normalizeBulkInventoryRows(Collections.singletonList(canonical));
                ValidationUtil.validateBulkInventoryRow(canonical);

                String normalizedBarcode = canonical[0];
                if (!seenBarcodes.add(normalizedBarcode)) {
                    throw new ApiException("Duplicate barcode in file");
                }

                InventoryPojo pojo = ProductHelper.toBulkInventoryPojo(canonical);
                validPojos.add(pojo);

                firstRowIndexByBarcode.put(pojo.getProductId(), rowIndex);
                resultRows.get(rowIndex)[0] = pojo.getProductId();
            } catch (ApiException e) {
                resultRows.get(rowIndex)[2] = e.getMessage();
            }
        }
        return new RowProcessingResult<>(validPojos, resultRows, firstRowIndexByBarcode);
    }

    private void applyFlowResultsToRows(RowProcessingResult<?> processed, List<String[]> flowResults) {
        if (flowResults == null || flowResults.isEmpty()) return;

        for (String[] r : flowResults) {
            // TODO: no use of this if
            if (r == null || r.length < 3) continue;
            Integer idx = processed.firstRowIndexByBarcode().get(r[0]);
            if (idx == null) continue;
            processed.resultRows().set(idx, r);
        }
    }

    private String[] toCanonicalProductRow(String[] rawRow, Map<String, Integer> headers) {
        boolean hasImage = headers.containsKey("imageurl");
        String barcode = cell(rawRow, headers, "barcode");
        String clientEmail = cell(rawRow, headers, "clientemail");
        String name = cell(rawRow, headers, "name");
        String mrp = cell(rawRow, headers, "mrp");

        if (!hasImage) return new String[]{barcode, clientEmail, name, mrp};

        String imageUrl = cell(rawRow, headers, "imageurl");
        return new String[]{barcode, clientEmail, name, mrp, imageUrl};
    }

    private String[] toCanonicalInventoryRow(String[] rawRow, Map<String, Integer> headers) {
        String barcode = cell(rawRow, headers, "barcode");
        String inventory = cell(rawRow, headers, "inventory");
        return new String[]{barcode, inventory};
    }

    private String cell(String[] row, Map<String, Integer> headers, String col) {
        Integer idx = headers.get(col);
        if (idx == null) return "";
        if (row == null) return "";
        if (idx < 0 || idx >= row.length) return "";
        String v = row[idx];
        return v == null ? "" : v;
    }

    private BulkUploadData encodeResult(List<String[]> resultRows) {
        return new BulkUploadData(TsvHelper.encodeResult(resultRows));
    }

    private String[] errorResult(String barcode) {
        return new String[]{barcode == null ? "" : barcode, "ERROR", ""};
    }

    private record ParsedBulkData(Map<String, Integer> headers, List<String[]> rows) {}

    private record RowProcessingResult<T>(
            List<T> validPojos,
            List<String[]> resultRows,
            Map<String, Integer> firstRowIndexByBarcode
    ) {}
}
