package com.increff.pos.util;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public class ValidationUtil {

    private static final int EMAIL_MAX = 40;
    private static final int NAME_MAX = 30;

    public static void validateClientForm(ClientForm form) throws ApiException {
        if (form == null) throw new ApiException("Client form required");
        validateEmail(form.getEmail());
        validateName(form.getName());
    }

    public static void validateClientUpdateForm(ClientUpdateForm form) throws ApiException {
        if (form == null) throw new ApiException("Client update form required");
        validateEmail(form.getOldEmail());
        validateEmail(form.getNewEmail());
        validateName(form.getName());
    }

    public static void validateClientFilterForm(ClientFilterForm form) throws ApiException {
        if (form == null) throw new ApiException("Client filter form required");
        validateOptionalName(form.getName());
        validateOptionalEmail(form.getEmail());
        validatePageBounds(form.getPage(), form.getSize());
    }

    public static void validateProductForm(ProductForm form) throws ApiException {
        if (form == null) throw new ApiException("Product form required");

        validateProductCommon(
                form.getBarcode(),
                form.getClientEmail(),
                form.getName(),
                form.getMrp(),
                form.getImageUrl()
        );
    }

    public static void validateProductUpdateForm(ProductUpdateForm form) throws ApiException {
        if (form == null) throw new ApiException("Product update form required");

        if (!StringUtils.hasText(form.getOldBarcode())) {
            throw new ApiException("Old barcode cannot be empty");
        }

        validateProductCommon(
                form.getNewBarcode(),
                form.getClientEmail(),
                form.getName(),
                form.getMrp(),
                form.getImageUrl()
        );
    }

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
        if (!StringUtils.hasText(clientEmail)) {
            throw new ApiException("Client is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new ApiException("Product name cannot be empty");
        }
        if (mrp == null || mrp <= 0) {
            throw new ApiException("Invalid MRP");
        }

        validateOptionalImageUrl(imageUrl);
    }

    public static void validateInventoryUpdateForm(InventoryUpdateForm form) throws ApiException {
        if (form == null) throw new ApiException("Inventory update form required");

        if (!StringUtils.hasText(form.getProductId())) {
            throw new ApiException("Product id cannot be empty");
        }
        if (form.getQuantity() == null || form.getQuantity() < 0) {
            throw new ApiException("Invalid inventory quantity");
        }
    }

    public static void validatePageForm(PageForm form) throws ApiException {
        if (form == null) throw new ApiException("Page form required");

        if (form.getPage() < 0) throw new ApiException("Page number cannot be negative");
        if (form.getSize() <= 0 || form.getSize() > 100)
            throw new ApiException("Invalid page size");
    }

    public static void validateBulkProductForm(List<String[]> rows) throws ApiException {
        if (rows == null || rows.isEmpty()) throw new ApiException("File is empty");

        for (String[] row : rows) {
            if (row.length == 0) continue;
            if ("barcode".equalsIgnoreCase(row[0])) continue;
            validateBulkProductRow(row);
        }
    }

    public static void validateBulkInventoryForm(List<String[]> rows) throws ApiException {
        if (rows == null || rows.isEmpty()) throw new ApiException("File is empty");

        for (String[] row : rows) {
            if (row.length == 0) continue;
            if ("barcode".equalsIgnoreCase(row[0])) continue;
            validateBulkInventoryRow(row);
        }
    }

    public static void validateBulkProductRow(String[] row) throws ApiException {
        if (row == null || row.length < 4 || row.length > 5) {
            throw new ApiException("Expected columns: barcode, clientEmail, name, mrp, imageUrl(optional)");
        }

        validateProductCommon(
                row[0],
                row[1],
                row[2],
                parseMrp(row[3]),
                row.length == 5 ? row[4] : null
        );
    }

    public static void validateBulkInventoryRow(String[] row) throws ApiException {
        if (row == null || row.length != 2) {
            throw new ApiException("Expected columns: barcode and quantity");
        }

        try {
            int qty = Integer.parseInt(row[1]);
            if (qty < 0) throw new ApiException("Quantity cannot be negative");
        } catch (NumberFormatException e) {
            throw new ApiException("Quantity must be a valid integer");
        }
    }

    public static void validateProductFilterForm(ProductFilterForm form) throws ApiException {
        if (form == null) throw new ApiException("Filter form required");

        if (form.getBarcode() != null && form.getBarcode().length() > 40)
            throw new ApiException("Barcode filter too long");

        if (form.getName() != null && form.getName().length() > NAME_MAX)
            throw new ApiException("Name filter too long");

        if (form.getClient() != null && form.getClient().length() > EMAIL_MAX)
            throw new ApiException("Client filter too long");

        validatePageBounds(form.getPage(), form.getSize());
    }

    private static Double parseMrp(String v) throws ApiException {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            throw new ApiException("MRP must be a number");
        }
    }

    public static void validateEmail(String email) throws ApiException {
        if (!StringUtils.hasText(email)) throw new ApiException("Email required");
        if (email.length() > EMAIL_MAX) throw new ApiException("Email too long");

        // Simple regex to validate email format
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailRegex)) {
            throw new ApiException("Invalid email format");
        }
    }

    private static void validateName(String name) throws ApiException {
        if (!StringUtils.hasText(name)) throw new ApiException("Name required");
        if (name.length() > NAME_MAX) throw new ApiException("Name too long");
    }

    private static void validateOptionalName(String name) throws ApiException {
        if (name != null && name.length() > NAME_MAX)
            throw new ApiException("Name filter too long");
    }

    private static void validateOptionalEmail(String email) throws ApiException {
        if (email != null && email.length() > EMAIL_MAX)
            throw new ApiException("Email filter too long");
    }

    private static void validateOptionalImageUrl(String url) throws ApiException {
        if (url == null || url.isBlank()) return;
        if (url.length() > 500) throw new ApiException("Image URL too long");
    }

    private static void validatePageBounds(int page, int size) throws ApiException {
        if (page < 0) throw new ApiException("Page cannot be negative");
        if (size <= 0 || size > 100) throw new ApiException("Invalid page size");
    }

    public static List<ProductPojo> convertBulkProductRowsToPojos(List<String[]> rows)
            throws ApiException {

        return rows.stream().map(r -> {
            ProductPojo p = new ProductPojo();
            p.setBarcode(r[0]);
            p.setClientEmail(r[1]);
            p.setName(r[2]);
            p.setMrp(Double.parseDouble(r[3]));
            if (r.length == 5) p.setImageUrl(r[4]);
            return p;
        }).toList();
    }

    public static List<InventoryPojo> convertBulkInventoryRowsToPojos(List<String[]> rows)
            throws ApiException {

        return rows.stream().map(r -> {
            InventoryPojo i = new InventoryPojo();
            i.setProductId(r[0]); // barcode for now
            i.setQuantity(Integer.parseInt(r[1]));
            return i;
        }).toList();
    }

    // ---------------- BULK VALIDATION ----------------

    public static void validateBulkProductData(Map<String, Integer> headers, List<String[]> rows) throws ApiException {
        List<String> requiredCols = List.of("barcode", "clientemail", "name", "mrp");
        for (String col : requiredCols) {
            if (!headers.containsKey(col)) {
                throw new ApiException("Missing required column: " + col);
            }
        }
    }

    public static void validateBulkInventoryData(Map<String, Integer> headers, List<String[]> rows) throws ApiException {
        if (!headers.containsKey("barcode") || !headers.containsKey("inventory")) {
            throw new ApiException("Required columns: barcode, inventory");
        }
    }

    public static void validateOrderFilterForm(OrderFilterForm form) throws ApiException {
        if (form == null) throw new ApiException("Order filter form required");
        validatePageBounds(form.getPage(), form.getSize());
        if (StringUtils.hasText(form.getOrderReferenceId()) && form.getOrderReferenceId().length() > 50) {
            throw new ApiException("Order reference id filter too long");
        }
        if (StringUtils.hasText(form.getStatus()) && form.getStatus().length() > 30) {
            throw new ApiException("Status filter too long");
        }
    }


}
