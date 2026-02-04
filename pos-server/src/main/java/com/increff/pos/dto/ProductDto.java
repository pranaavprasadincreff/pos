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

        ProductPojo productToCreate = ProductHelper.convertProductFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> created = productFlow.addProduct(productToCreate);

        return ProductHelper.convertToProductData(created.getLeft(), created.getRight());
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        String normalizedBarcode = NormalizationUtil.normalizeBarcode(barcode);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.getByBarcode(normalizedBarcode);
        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public Page<ProductData> getAllUsingFilter(PageForm form) throws ApiException {
        return filterAllProducts(form);
    }

    public Page<ProductData> filter(ProductFilterForm form) throws ApiException {
        NormalizationUtil.normalizeProductFilterForm(form);
        ValidationUtil.validateProductFilterForm(form);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.filter(form);
        return convertToProductDataPage(page);
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeProductUpdateForm(form);
        ValidationUtil.validateProductUpdateForm(form);

        ProductUpdatePojo update = ProductHelper.convertProductUpdateFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateProduct(update);

        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeInventoryUpdateForm(form);
        ValidationUtil.validateInventoryUpdateForm(form);

        InventoryUpdatePojo update = ProductHelper.convertInventoryUpdateFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateInventory(update);

        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        ParsedTsvData parsed = parseAndValidateProductBulkHeaders(form);

        List<ProductPojo> alignedInput = new ArrayList<>(parsed.rows().size());
        Map<Integer, String> dtoErrorsByRowIndex = new HashMap<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            String[] rawRow = parsed.rows().get(i);

            try {
                String[] canonical = toCanonicalProductRow(rawRow, parsed.headers());
                NormalizationUtil.normalizeBulkProductRows(Collections.singletonList(canonical));
                ValidationUtil.validateBulkProductRow(canonical);

                ProductPojo pojo = ProductHelper.toBulkProductPojo(canonical);
                alignedInput.add(pojo);
            } catch (ApiException e) {
                alignedInput.add(null);
                dtoErrorsByRowIndex.put(i, e.getMessage());
            }
        }

        // Flow returns one result row per input row (same order)
        List<String[]> flowResults = productFlow.bulkAddProducts(alignedInput);

        // Override rows that failed DTO-level parse/validation
        applyDtoErrors(flowResults, alignedInput, dtoErrorsByRowIndex);

        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }

    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        ParsedTsvData parsed = parseAndValidateInventoryBulkHeaders(form);

        List<InventoryPojo> alignedInput = new ArrayList<>(parsed.rows().size());
        Map<Integer, String> dtoErrorsByRowIndex = new HashMap<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            String[] rawRow = parsed.rows().get(i);

            try {
                String[] canonical = toCanonicalInventoryRow(rawRow, parsed.headers());
                NormalizationUtil.normalizeBulkInventoryRows(Collections.singletonList(canonical));
                ValidationUtil.validateBulkInventoryRow(canonical);

                InventoryPojo pojo = ProductHelper.toBulkInventoryPojo(canonical);
                alignedInput.add(pojo); // productId == barcode carrier
            } catch (ApiException e) {
                alignedInput.add(null);
                dtoErrorsByRowIndex.put(i, e.getMessage());
            }
        }

        // Flow supports duplicate barcode rows by aggregating deltas ✅
        List<String[]> flowResults = productFlow.bulkUpdateInventory(alignedInput);

        // Override rows that failed DTO-level parse/validation
        applyDtoErrors(flowResults, alignedInput, dtoErrorsByRowIndex);

        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }

    // -------------------- Private helpers --------------------

    private Page<ProductData> filterAllProducts(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);

        ProductFilterForm filter = new ProductFilterForm();
        filter.setBarcode(null);
        filter.setName(null);
        filter.setClient(null);
        filter.setPage(form.getPage());
        filter.setSize(form.getSize());

        return filter(filter);
    }

    private ParsedTsvData parseAndValidateProductBulkHeaders(BulkUploadForm form) throws ApiException {
        var parsed = TsvHelper.parse(form.getFile());
        ValidationUtil.validateBulkProductData(parsed.getLeft(), parsed.getRight());
        return new ParsedTsvData(parsed.getLeft(), parsed.getRight());
    }

    private ParsedTsvData parseAndValidateInventoryBulkHeaders(BulkUploadForm form) throws ApiException {
        var parsed = TsvHelper.parse(form.getFile());
        ValidationUtil.validateBulkInventoryData(parsed.getLeft(), parsed.getRight());
        return new ParsedTsvData(parsed.getLeft(), parsed.getRight());
    }

    private Page<ProductData> convertToProductDataPage(Page<Pair<ProductPojo, InventoryPojo>> page) {
        List<ProductData> data = page.getContent()
                .stream()
                .map(pair -> ProductHelper.convertToProductData(pair.getLeft(), pair.getRight()))
                .toList();
        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    private String[] toCanonicalProductRow(String[] rawRow, Map<String, Integer> headers) {
        boolean hasImageUrl = headers.containsKey("imageurl");

        String barcode = readCell(rawRow, headers, "barcode");
        String clientEmail = readCell(rawRow, headers, "clientemail");
        String name = readCell(rawRow, headers, "name");
        String mrp = readCell(rawRow, headers, "mrp");

        if (!hasImageUrl) return new String[]{barcode, clientEmail, name, mrp};

        String imageUrl = readCell(rawRow, headers, "imageurl");
        return new String[]{barcode, clientEmail, name, mrp, imageUrl};
    }

    private String[] toCanonicalInventoryRow(String[] rawRow, Map<String, Integer> headers) {
        String barcode = readCell(rawRow, headers, "barcode");
        String inventory = readCell(rawRow, headers, "inventory");
        return new String[]{barcode, inventory};
    }

    private String readCell(String[] row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName);
        if (index == null || row == null || index < 0 || index >= row.length) return "";
        return row[index] == null ? "" : row[index];
    }

    private void applyDtoErrors(
            List<String[]> flowResults,
            List<?> alignedInput,
            Map<Integer, String> dtoErrorsByRowIndex
    ) {
        for (Map.Entry<Integer, String> e : dtoErrorsByRowIndex.entrySet()) {
            int idx = e.getKey();
            String msg = e.getValue();

            String barcode = "";
            Object rowPojo = (idx >= 0 && idx < alignedInput.size()) ? alignedInput.get(idx) : null;
            // We lost pojo for invalid rows; try pull barcode from flow row if present
            if (idx >= 0 && idx < flowResults.size() && flowResults.get(idx) != null && flowResults.get(idx).length > 0) {
                barcode = flowResults.get(idx)[0] == null ? "" : flowResults.get(idx)[0];
            }

            flowResults.set(idx, new String[]{barcode, "ERROR", msg});
        }
    }

    private record ParsedTsvData(Map<String, Integer> headers, List<String[]> rows) {}
}
