package com.increff.pos.util;

import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateItemForm;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ProductSearchForm;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final int EMAIL_MAX = 40;
    private static final int NAME_MAX = 30;
    private static final int BULK_MAX_ROWS = 5000;
    private static final int INVENTORY_MAX = 1000;
    private static final int BARCODE_MAX = 40;

    private static final int ORDER_REFERENCE_ID_MAX = 50;
    private static final int STATUS_MAX = 15;

    private static final Set<String> ORDER_STATUS_ALLOWED = Set.of(
            "FULFILLABLE",
            "UNFULFILLABLE",
            "INVOICED",
            "CANCELLED"
    );

    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ValidationUtil() {}

    // ---------------- BASIC ----------------

    public static void validateEmail(String email) throws ApiException {
        if (!StringUtils.hasText(email)) {
            throw new ApiException("Email is required");
        }
        if (email.length() > EMAIL_MAX) {
            throw new ApiException("Email too long");
        }
        if (!SIMPLE_EMAIL.matcher(email).matches()) {
            throw new ApiException("Invalid email format");
        }
    }

    public static void validateBarcode(String barcode) throws ApiException {
        if (!StringUtils.hasText(barcode)) {
            throw new ApiException("Barcode is required");
        }
        if (barcode.length() > BARCODE_MAX) {
            throw new ApiException("Barcode too long");
        }
    }

    // ---------------- getCheck parsers (single entry-point for parsing) ----------------

    public static Double getCheckMrp(String rawMrp) throws ApiException {
        if (!StringUtils.hasText(rawMrp)) {
            throw new ApiException("MRP is required");
        }
        try {
            return Double.parseDouble(rawMrp);
        } catch (Exception e) {
            throw new ApiException("MRP must be a number");
        }
    }

    public static Integer getCheckInventoryDelta(String rawQty) throws ApiException {
        if (!StringUtils.hasText(rawQty)) {
            throw new ApiException("Quantity is required");
        }
        try {
            return Integer.parseInt(rawQty);
        } catch (Exception e) {
            throw new ApiException("Quantity must be a valid integer");
        }
    }

    // ---------------- PRODUCT COMMON (no parsing here) ----------------

    private static void validateProductCommon(
            String barcode,
            String clientEmail,
            String name,
            Double mrp,
            String imageUrl
    ) throws ApiException {

        if (!StringUtils.hasText(barcode)) {
            throw new ApiException("Barcode cannot be empty");
        }
        if (barcode.length() > BARCODE_MAX) {
            throw new ApiException("Barcode too long");
        }

        if (!StringUtils.hasText(clientEmail)) {
            throw new ApiException("Client is required");
        }
        if (clientEmail.length() > EMAIL_MAX) {
            throw new ApiException("Email too long");
        }

        if (!StringUtils.hasText(name)) {
            throw new ApiException("Product name cannot be empty");
        }
        if (name.length() > NAME_MAX) {
            throw new ApiException("Name too long");
        }

        if (mrp == null) {
            throw new ApiException("MRP is required");
        }
        if (mrp <= 0) {
            throw new ApiException("Invalid MRP");
        }

        validateOptionalImageUrl(imageUrl);
    }

    // ---------------- BULK (row-level) ----------------

    public static void validateBulkProductRow(String[] row) throws ApiException {
        if (row == null || row.length < 4 || row.length > 5) {
            throw new ApiException("Expected columns: barcode, clientEmail, name, mrp, imageUrl(optional)");
        }

        Double mrp = getCheckMrp(row[3]);

        validateProductCommon(
                row[0],
                row[1],
                row[2],
                mrp,
                row.length == 5 ? row[4] : null
        );
    }

    public static void validateBulkInventoryRow(String[] row) throws ApiException {
        if (row == null || row.length != 2) {
            throw new ApiException("Expected columns: barcode and quantity");
        }

        String barcode = row[0];
        if (!StringUtils.hasText(barcode)) {
            throw new ApiException("Barcode cannot be empty");
        }
        if (barcode.length() > BARCODE_MAX) {
            throw new ApiException("Barcode too long");
        }

        getCheckInventoryDelta(row[1]);
    }

    // ---------------- BULK (file-level) ----------------

    public static void validateBulkProductData(Map<String, Integer> headers, List<String[]> rows) throws ApiException {
        validateBulkRowLimit(rows);

        List<String> requiredCols = List.of("barcode", "clientemail", "name", "mrp");
        for (String col : requiredCols) {
            if (!headers.containsKey(col)) {
                throw new ApiException("Missing required column: " + col);
            }
        }
    }

    public static void validateBulkInventoryData(Map<String, Integer> headers, List<String[]> rows) throws ApiException {
        validateBulkRowLimit(rows);

        if (!headers.containsKey("barcode") || !headers.containsKey("inventory")) {
            throw new ApiException("Required columns: barcode, inventory");
        }
    }

    private static void validateBulkRowLimit(List<String[]> rows) throws ApiException {
        if (rows == null) {
            throw new ApiException("File is empty");
        }
        if (rows.size() > BULK_MAX_ROWS) {
            throw new ApiException("Bulk upload supports at most " + BULK_MAX_ROWS + " rows");
        }
    }

    // ---------------- ORDER ----------------

    public static void validateOrderReferenceId(String ref) throws ApiException {
        if (!StringUtils.hasText(ref)) {
            throw new ApiException("Order reference id is required");
        }
        if (ref.trim().length() > ORDER_REFERENCE_ID_MAX) {
            throw new ApiException("Order reference id too long");
        }
    }

    public static void validateOrderCreateForm(OrderCreateForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Order form required");
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new ApiException("At least one order item required");
        }

        for (OrderCreateItemForm item : form.getItems()) {
            validateOrderItem(item);
        }
    }

    private static void validateOrderItem(OrderCreateItemForm item) throws ApiException {
        if (item == null) {
            throw new ApiException("Order item required");
        }

        if (!StringUtils.hasText(item.getProductBarcode())) {
            throw new ApiException("Barcode required");
        }
        if (item.getProductBarcode().length() > BARCODE_MAX) {
            throw new ApiException("Barcode too long");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ApiException("Quantity must be > 0");
        }
        if (item.getQuantity() > INVENTORY_MAX) {
            throw new ApiException("Quantity cannot exceed " + INVENTORY_MAX);
        }

        if (item.getSellingPrice() == null || item.getSellingPrice() <= 0) {
            throw new ApiException("Selling price must be > 0");
        }
    }

    // ---------------- COMMON ----------------

    private static void validateOptionalImageUrl(String url) throws ApiException {
        if (url == null || url.isBlank()) return;
        if (url.length() > 500) {
            throw new ApiException("Image URL too long");
        }
    }

    public static void validatePageForm(PageForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Page form required");
        }
        if (form.getPage() < 0) {
            throw new ApiException("Page number cannot be negative");
        }
        if (form.getSize() <= 0 || form.getSize() > 100) {
            throw new ApiException("Invalid page size");
        }
    }

    public static void validateProductFilterForm(ProductSearchForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Filter form required");
        }

        if (form.getBarcode() != null && form.getBarcode().length() > BARCODE_MAX) {
            throw new ApiException("Barcode filter too long");
        }

        if (form.getName() != null && form.getName().length() > NAME_MAX) {
            throw new ApiException("Name filter too long");
        }

        if (form.getClient() != null && form.getClient().length() > EMAIL_MAX) {
            throw new ApiException("Client filter too long");
        }

        validatePageBounds(form.getPage(), form.getSize());
    }

    private static void validatePageBounds(int page, int size) throws ApiException {
        if (page < 0) throw new ApiException("Page cannot be negative");
        if (size <= 0 || size > 100) throw new ApiException("Invalid page size");
    }

    public static void validateToken(String token) throws ApiException {
        if (token == null || token.isBlank()) {
            throw new ApiException("Missing or invalid token");
        }
    }

    public static void validateOrderUpdateForm(com.increff.pos.model.form.OrderUpdateForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Order update form required");
        }
        validateOrderReferenceId(form.getOrderReferenceId());

        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new ApiException("At least one order item required");
        }

        for (OrderCreateItemForm item : form.getItems()) {
            validateOrderItem(item);
        }
    }

    public static void validateOrderSearchForm(com.increff.pos.model.form.OrderSearchForm form) throws ApiException {
        if (form == null) {
            throw new ApiException("Search form required");
        }
        validatePageBounds(form.getPage(), form.getSize());

        if (form.getOrderReferenceId() != null) {
            validateOrderReferenceId(form.getOrderReferenceId());
        }

        if (form.getStatus() != null) {
            String normalizedStatus = form.getStatus().trim().toUpperCase();
            if (normalizedStatus.length() > STATUS_MAX) {
                throw new ApiException("Status too long");
            }
            if (!ORDER_STATUS_ALLOWED.contains(normalizedStatus)) {
                throw new ApiException("Invalid status");
            }
        }
    }

}
