package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.helper.BulkUploadHelper;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.helper.TsvHelper;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import com.increff.pos.util.FormValidationUtil;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProductDto {

    @Autowired
    private ProductFlow productFlow;

    public ProductData addProduct(ProductForm form) throws ApiException {
        NormalizationUtil.normalizeProductForm(form);
        FormValidationUtil.validate(form);

        ProductPojo productToCreate = ProductHelper.convertProductFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> createdProduct = productFlow.addProduct(productToCreate);
        return ProductHelper.convertToProductData(createdProduct);
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        String normalizedBarcode = NormalizationUtil.normalizeBarcode(barcode);
        ValidationUtil.validateBarcode(normalizedBarcode);

        Pair<ProductPojo, InventoryPojo> product = productFlow.getByBarcode(normalizedBarcode);
        return ProductHelper.convertToProductData(product);
    }

    public Page<ProductData> search(ProductSearchForm form) throws ApiException {
        NormalizationUtil.normalizeProductSearchForm(form);
        FormValidationUtil.validate(form);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.search(form);
        return ProductHelper.convertToProductDataPage(page);
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeProductUpdateForm(form);
        FormValidationUtil.validate(form);

        Pair<ProductPojo, InventoryPojo> existing = productFlow.getByBarcode(form.getOldBarcode());
        ProductPojo productToUpdate = ProductHelper.convertProductUpdateFormToProductPojo(existing, form);

        Pair<ProductPojo, InventoryPojo> updated = productFlow.updateProduct(productToUpdate);
        return ProductHelper.convertToProductData(updated);
    }

    // ---------------- BULK ----------------

    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        FormValidationUtil.validate(form);

        ParsedBulkFile parsedBulkFile = parseProductBulkFile(form);
        Map<String, Integer> columnNameIndexMap = parsedBulkFile.headers();
        List<String[]> tsvRowList = parsedBulkFile.rows();

        Map<String, Integer> columnNameColumnIndexMap = getCheckProductColumnNameColumnIndexMap(columnNameIndexMap);
        int barcodeIndex = columnNameColumnIndexMap.get("barcode");
        int clientEmailIndex = columnNameColumnIndexMap.get("clientemail");
        int nameIndex = columnNameColumnIndexMap.get("name");
        int mrpIndex = columnNameColumnIndexMap.get("mrp");
        Integer imageUrlIndex = columnNameColumnIndexMap.get("imageurl"); // optional

        List<ProductPojo> productPojoByRowIndexList = new ArrayList<>(tsvRowList.size());
        List<String> barcodeByRowIndexList = new ArrayList<>(tsvRowList.size());
        Map<Integer, String> rowIndexErrorMessageMap = new HashMap<>();

        for (int rowIndex = 0; rowIndex < tsvRowList.size(); rowIndex++) {
            String[] tsvRow = tsvRowList.get(rowIndex);

            try {
                String[] canonicalRow = getCanonicalBulkProductRow(
                        tsvRow, barcodeIndex, clientEmailIndex, nameIndex, mrpIndex, imageUrlIndex
                );

                NormalizationUtil.normalizeBulkProductRow(canonicalRow);
                ValidationUtil.validateBulkProductRow(canonicalRow);

                Double mrp = ValidationUtil.getCheckMrp(canonicalRow[3]);
                ProductPojo productPojo = ProductHelper.toBulkProductPojo(canonicalRow, mrp);

                barcodeByRowIndexList.add(productPojo.getBarcode());
                productPojoByRowIndexList.add(productPojo);

            } catch (ApiException e) {
                barcodeByRowIndexList.add(getNormalizedBarcodeForResult(tsvRow, barcodeIndex));
                productPojoByRowIndexList.add(null);
                rowIndexErrorMessageMap.put(rowIndex, e.getMessage());
            }
        }

        List<String[]> resultRows = productFlow.bulkAddProducts(productPojoByRowIndexList);

        enforceBarcodeColumn(resultRows, barcodeByRowIndexList);
        BulkUploadHelper.applyRowErrors(resultRows, rowIndexErrorMessageMap);

        return new BulkUploadData(TsvHelper.encodeResult(resultRows));
    }

    // -------------------- private helpers --------------------

    private ParsedBulkFile parseProductBulkFile(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = BulkUploadHelper.parseProductFile(form.getFile());
        return new ParsedBulkFile(parsed.getLeft(), parsed.getRight());
    }

    private Map<String, Integer> getCheckProductColumnNameColumnIndexMap(Map<String, Integer> columnNameIndexMap) throws ApiException {
        Integer barcodeIndex = columnNameIndexMap.get("barcode");
        Integer clientEmailIndex = columnNameIndexMap.get("clientemail");
        Integer nameIndex = columnNameIndexMap.get("name");
        Integer mrpIndex = columnNameIndexMap.get("mrp");
        Integer imageUrlIndex = columnNameIndexMap.get("imageurl"); // optional

        if (barcodeIndex == null || clientEmailIndex == null || nameIndex == null || mrpIndex == null) {
            throw new ApiException("Missing required columns: barcode, clientemail, name, mrp");
        }

        Map<String, Integer> columnNameColumnIndexMap = new HashMap<>();
        columnNameColumnIndexMap.put("barcode", barcodeIndex);
        columnNameColumnIndexMap.put("clientemail", clientEmailIndex);
        columnNameColumnIndexMap.put("name", nameIndex);
        columnNameColumnIndexMap.put("mrp", mrpIndex);
        if (imageUrlIndex != null) {
            columnNameColumnIndexMap.put("imageurl", imageUrlIndex);
        }
        return columnNameColumnIndexMap;
    }

    private String[] getCanonicalBulkProductRow(
            String[] tsvRow,
            int barcodeIndex,
            int clientEmailIndex,
            int nameIndex,
            int mrpIndex,
            Integer imageUrlIndex
    ) {
        String rawBarcode = readCell(tsvRow, barcodeIndex);
        String rawClientEmail = readCell(tsvRow, clientEmailIndex);
        String rawName = readCell(tsvRow, nameIndex);
        String rawMrp = readCell(tsvRow, mrpIndex);

        if (imageUrlIndex == null) {
            return new String[]{rawBarcode, rawClientEmail, rawName, rawMrp};
        }

        String rawImageUrl = readCell(tsvRow, imageUrlIndex);
        return new String[]{rawBarcode, rawClientEmail, rawName, rawMrp, rawImageUrl};
    }

    private String getNormalizedBarcodeForResult(String[] tsvRow, int barcodeIndex) {
        return NormalizationUtil.normalizeBarcode(readCell(tsvRow, barcodeIndex));
    }

    private String readCell(String[] tsvRow, int index) {
        if (tsvRow == null) return "";
        if (index < 0 || index >= tsvRow.length) return "";
        String value = tsvRow[index];
        return value == null ? "" : value;
    }

    private void enforceBarcodeColumn(List<String[]> resultRows, List<String> barcodeByRowIndexList) {
        int rowsToUpdate = Math.min(resultRows.size(), barcodeByRowIndexList.size());
        for (int rowIndex = 0; rowIndex < rowsToUpdate; rowIndex++) {
            String[] resultRow = resultRows.get(rowIndex);
            if (resultRow == null || resultRow.length < 1) continue;

            String barcode = barcodeByRowIndexList.get(rowIndex);
            resultRow[0] = barcode == null ? "" : barcode;
        }
    }

    private record ParsedBulkFile(Map<String, Integer> headers, List<String[]> rows) { }
}
