package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.flow.InventoryFlow;
import com.increff.pos.helper.BulkUploadHelper;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.helper.TsvHelper;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.util.FormValidationUtil;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InventoryDto {

    @Autowired
    private InventoryFlow inventoryFlow;

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeInventoryUpdateForm(form);
        FormValidationUtil.validate(form);

        Pair<ProductPojo, InventoryPojo> existing = inventoryFlow.getByBarcode(form.getBarcode());
        InventoryPojo inventoryToUpdate = ProductHelper.convertInventoryUpdateFormToInventoryPojo(existing, form);

        Pair<ProductPojo, InventoryPojo> updated = inventoryFlow.updateInventory(inventoryToUpdate);
        return ProductHelper.convertToProductData(updated);
    }

    // ---------------- BULK ----------------

    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        FormValidationUtil.validate(form);

        ParsedBulkFile parsedBulkFile = parseInventoryBulkFile(form);
        List<String[]> tsvRowList = parsedBulkFile.rows();

        Map<String, Integer> columnNameColumnIndexMap = getCheckInventoryColumnNameColumnIndexMap(parsedBulkFile.headers());
        int barcodeIndex = columnNameColumnIndexMap.get("barcode");
        int inventoryIndex = columnNameColumnIndexMap.get("inventory");

        List<InventoryPojo> inventoryPojoByRowIndexList = new ArrayList<>(tsvRowList.size());
        List<String> barcodeByRowIndexList = new ArrayList<>(tsvRowList.size());
        Map<Integer, String> rowIndexErrorMessageMap = new HashMap<>();

        for (int rowIndex = 0; rowIndex < tsvRowList.size(); rowIndex++) {
            String[] tsvRow = tsvRowList.get(rowIndex);

            try {
                String[] canonicalRow = getCanonicalBulkInventoryRow(tsvRow, barcodeIndex, inventoryIndex);

                NormalizationUtil.normalizeBulkInventoryRow(canonicalRow);
                ValidationUtil.validateBulkInventoryRow(canonicalRow);

                InventoryPojo inventoryDeltaPojo = ProductHelper.toBulkInventoryDeltaPojo(canonicalRow);

                barcodeByRowIndexList.add(inventoryDeltaPojo.getProductId());
                inventoryPojoByRowIndexList.add(inventoryDeltaPojo);

            } catch (ApiException e) {
                barcodeByRowIndexList.add(getNormalizedBarcodeForResult(tsvRow, barcodeIndex));
                inventoryPojoByRowIndexList.add(null);
                rowIndexErrorMessageMap.put(rowIndex, e.getMessage());
            }
        }

        List<String[]> resultRows = inventoryFlow.bulkUpdateInventory(inventoryPojoByRowIndexList);

        enforceBarcodeColumn(resultRows, barcodeByRowIndexList);
        BulkUploadHelper.applyRowErrors(resultRows, rowIndexErrorMessageMap);

        return new BulkUploadData(TsvHelper.encodeResult(resultRows));
    }

    // -------------------- private helpers --------------------

    private ParsedBulkFile parseInventoryBulkFile(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = BulkUploadHelper.parseInventoryFile(form.getFile());
        return new ParsedBulkFile(parsed.getLeft(), parsed.getRight());
    }

    private Map<String, Integer> getCheckInventoryColumnNameColumnIndexMap(Map<String, Integer> columnNameIndexMap) throws ApiException {
        Integer barcodeIndex = columnNameIndexMap.get("barcode");
        Integer inventoryIndex = columnNameIndexMap.get("inventory");

        if (barcodeIndex == null || inventoryIndex == null) {
            throw new ApiException("Required columns: barcode, inventory");
        }

        Map<String, Integer> columnNameColumnIndexMap = new HashMap<>();
        columnNameColumnIndexMap.put("barcode", barcodeIndex);
        columnNameColumnIndexMap.put("inventory", inventoryIndex);
        return columnNameColumnIndexMap;
    }

    private String[] getCanonicalBulkInventoryRow(String[] tsvRow, int barcodeIndex, int inventoryIndex) {
        String rawBarcode = readCell(tsvRow, barcodeIndex);
        String rawInventory = readCell(tsvRow, inventoryIndex);
        return new String[]{rawBarcode, rawInventory};
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
