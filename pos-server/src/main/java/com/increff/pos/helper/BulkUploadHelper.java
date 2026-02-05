package com.increff.pos.helper;

import com.increff.pos.model.exception.ApiException;
import com.increff.pos.util.NormalizationUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class BulkUploadHelper {

    private static final int BULK_MAX_ROWS = 5000;

    private static final int BARCODE_MAX = 40;
    private static final int EMAIL_MAX = 40;
    private static final int NAME_MAX = 30;
    private static final int IMAGE_URL_MAX = 500;
    private static final int INVENTORY_MAX = 1000;

    private BulkUploadHelper() {
    }

    public static Pair<Map<String, Integer>, List<String[]>> parseProductFile(String base64FileContent) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = TsvHelper.parse(base64FileContent);
        validateRowLimit(parsed.getRight());
        validateProductHeaders(parsed.getLeft());
        return parsed;
    }

    public static Pair<Map<String, Integer>, List<String[]>> parseInventoryFile(String base64FileContent) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = TsvHelper.parse(base64FileContent);
        validateRowLimit(parsed.getRight());
        validateInventoryHeaders(parsed.getLeft());
        return parsed;
    }

    public static String readCell(String[] row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName);
        if (index == null) return "";
        if (row == null || index < 0 || index >= row.length) return "";
        String value = row[index];
        return value == null ? "" : value;
    }

    public static String normalizeBarcode(String value) {
        return NormalizationUtil.normalizeBarcode(value);
    }

    public static String normalizeEmail(String value) {
        return NormalizationUtil.normalizeEmail(value);
    }

    public static String normalizeName(String value) {
        return NormalizationUtil.normalizeName(value);
    }

    public static String normalizeUrl(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public static Double parseMrp(String rawMrp) throws ApiException {
        try {
            return Double.parseDouble(rawMrp);
        } catch (Exception e) {
            throw new ApiException("MRP must be a number");
        }
    }

    public static Integer parseInventoryDelta(String rawQuantity) throws ApiException {
        try {
            return Integer.parseInt(rawQuantity);
        } catch (Exception e) {
            throw new ApiException("Quantity must be a valid integer");
        }
    }

    public static void validateProductRow(
            String barcode,
            String clientEmail,
            String name,
            Double mrp,
            String imageUrl
    ) throws ApiException {

        if (isBlank(barcode)) throw new ApiException("Barcode cannot be empty");
        if (barcode.length() > BARCODE_MAX) throw new ApiException("Barcode too long");

        if (isBlank(clientEmail)) throw new ApiException("Client is required");
        if (clientEmail.length() > EMAIL_MAX) throw new ApiException("Email too long");

        if (isBlank(name)) throw new ApiException("Product name cannot be empty");
        if (name.length() > NAME_MAX) throw new ApiException("Name too long");

        if (mrp == null || mrp <= 0) throw new ApiException("Invalid MRP");

        if (imageUrl != null && imageUrl.length() > IMAGE_URL_MAX) throw new ApiException("Image URL too long");
    }

    public static void validateInventoryRow(String barcode, Integer delta) throws ApiException {
        if (isBlank(barcode)) throw new ApiException("Barcode cannot be empty");
        if (barcode.length() > BARCODE_MAX) throw new ApiException("Barcode too long");

        if (delta == null) throw new ApiException("Quantity is required");
        if (delta < 0) throw new ApiException("Quantity cannot be negative");
        if (delta > INVENTORY_MAX) throw new ApiException("Quantity cannot exceed " + INVENTORY_MAX);
    }

    public static void applyRowErrors(List<String[]> flowResults, Map<Integer, String> errorByRowIndex) {
        for (Map.Entry<Integer, String> entry : errorByRowIndex.entrySet()) {
            int rowIndex = entry.getKey();
            if (rowIndex < 0 || rowIndex >= flowResults.size()) continue;

            String message = entry.getValue();
            String[] existing = flowResults.get(rowIndex);

            String barcode = "";
            if (existing != null && existing.length > 0 && existing[0] != null) {
                barcode = existing[0];
            }

            flowResults.set(rowIndex, new String[]{barcode, "ERROR", message});
        }
    }

    // -------------------- private helpers --------------------

    private static void validateRowLimit(List<String[]> rows) throws ApiException {
        if (rows == null || rows.isEmpty()) throw new ApiException("File is empty");
        if (rows.size() > BULK_MAX_ROWS) throw new ApiException("Bulk upload supports at most " + BULK_MAX_ROWS + " rows");
    }

    private static void validateProductHeaders(Map<String, Integer> headers) throws ApiException {
        if (!headers.containsKey("barcode")) throw new ApiException("Missing required column: barcode");
        if (!headers.containsKey("clientemail")) throw new ApiException("Missing required column: clientemail");
        if (!headers.containsKey("name")) throw new ApiException("Missing required column: name");
        if (!headers.containsKey("mrp")) throw new ApiException("Missing required column: mrp");
    }

    private static void validateInventoryHeaders(Map<String, Integer> headers) throws ApiException {
        if (!headers.containsKey("barcode") || !headers.containsKey("inventory")) {
            throw new ApiException("Required columns: barcode, inventory");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
