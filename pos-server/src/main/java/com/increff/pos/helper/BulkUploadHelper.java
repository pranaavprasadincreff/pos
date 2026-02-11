package com.increff.pos.helper;

import com.increff.pos.model.exception.ApiException;
import com.increff.pos.util.ValidationUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class BulkUploadHelper {

    private BulkUploadHelper() {}

    public static Pair<Map<String, Integer>, List<String[]>> parseProductFile(String base64FileContent) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsedBulkFile = TsvHelper.parse(base64FileContent);

        Map<String, Integer> columnNameIndexMap = parsedBulkFile.getLeft();
        List<String[]> tsvRows = parsedBulkFile.getRight();

        ValidationUtil.validateBulkProductData(columnNameIndexMap, tsvRows);
        return parsedBulkFile;
    }

    public static Pair<Map<String, Integer>, List<String[]>> parseInventoryFile(String base64FileContent) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsedBulkFile = TsvHelper.parse(base64FileContent);

        Map<String, Integer> columnNameIndexMap = parsedBulkFile.getLeft();
        List<String[]> tsvRows = parsedBulkFile.getRight();

        ValidationUtil.validateBulkInventoryData(columnNameIndexMap, tsvRows);
        return parsedBulkFile;
    }

    public static String readCell(String[] tsvRow, Map<String, Integer> columnNameIndexMap, String columnName) {
        Integer index = columnNameIndexMap.get(columnName);
        if (index == null) return "";
        if (tsvRow == null || index < 0 || index >= tsvRow.length) return "";

        String value = tsvRow[index];
        return value == null ? "" : value;
    }

    public static void applyRowErrors(List<String[]> resultRows, Map<Integer, String> rowIndexErrorMessageMap) {
        if (resultRows == null || rowIndexErrorMessageMap == null || rowIndexErrorMessageMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, String> entry : rowIndexErrorMessageMap.entrySet()) {
            int rowIndex = entry.getKey();
            if (rowIndex < 0 || rowIndex >= resultRows.size()) continue;

            String errorMessage = entry.getValue();
            String[] existingRow = resultRows.get(rowIndex);

            String barcode = "";
            if (existingRow != null && existingRow.length > 0 && existingRow[0] != null) {
                barcode = existingRow[0];
            }

            resultRows.set(rowIndex, new String[]{barcode, "ERROR", errorMessage});
        }
    }
}
