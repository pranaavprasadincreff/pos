package com.increff.pos.util;

import com.increff.pos.model.form.*;
import org.springframework.util.StringUtils;

import java.util.List;

public class NormalizationUtil {

    private NormalizationUtil() {}

    public static void normalizeClientForm(ClientForm form) {
        if (form == null) return;
        form.setEmail(normalizeEmail(form.getEmail()));
        form.setName(normalizeName(form.getName()));
    }

    public static void normalizeClientUpdateForm(ClientUpdateForm form) {
        if (form == null) return;
        form.setOldEmail(normalizeEmail(form.getOldEmail()));
        form.setNewEmail(normalizeEmail(form.getNewEmail()));
        form.setName(normalizeName(form.getName()));
    }

    public static void normalizeClientSearchForm(ClientSearchForm form) {
        if (form == null) return;
        form.setName(normalizeName(form.getName()));
        form.setEmail(normalizeEmail(form.getEmail()));
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) return null;
        return email.trim().toLowerCase();
    }

    public static String normalizeName(String name) {
        if (!StringUtils.hasText(name)) return null;
        return name.trim().toLowerCase();
    }

    public static String normalizeBarcode(String barcode) {
        if (!StringUtils.hasText(barcode)) return null;
        return barcode.trim().toUpperCase();
    }

    public static void normalizeProductSearchForm(ProductSearchForm form) {
        if (form == null) return;
        form.setBarcode(normalizeBarcode(form.getBarcode()));
        form.setName(normalizeName(form.getName()));
        form.setClient(normalizeName(form.getClient()));
    }

    public static void normalizeProductForm(ProductForm form) {
        if (form == null) return;
        form.setBarcode(normalizeBarcode(form.getBarcode()));
        form.setClientEmail(normalizeEmail(form.getClientEmail()));
        form.setName(normalizeName(form.getName()));
    }

    public static void normalizeProductUpdateForm(ProductUpdateForm form) {
        if (form == null) return;
        form.setOldBarcode(normalizeBarcode(form.getOldBarcode()));
        form.setNewBarcode(normalizeBarcode(form.getNewBarcode()));
        form.setClientEmail(normalizeEmail(form.getClientEmail()));
        form.setName(normalizeName(form.getName()));
    }

    public static void normalizeBulkProductRow(String[] canonicalRow) {
        if (canonicalRow == null || canonicalRow.length < 4) return;

        canonicalRow[0] = normalizeBarcode(canonicalRow[0]);
        canonicalRow[1] = normalizeEmail(canonicalRow[1]);
        canonicalRow[2] = normalizeName(canonicalRow[2]);

        if (canonicalRow.length == 5 && canonicalRow[4] != null) {
            String trimmed = canonicalRow[4].trim();
            canonicalRow[4] = trimmed.isBlank() ? null : trimmed;
        }
    }

    public static void normalizeBulkInventoryRow(String[] canonicalRow) {
        if (canonicalRow == null || canonicalRow.length < 1) return;
        canonicalRow[0] = normalizeBarcode(canonicalRow[0]);
    }

    public static void normalizeOrderCreateForm(OrderCreateForm form) {
        if (form == null || form.getItems() == null) return;
        form.getItems().forEach(i -> i.setProductBarcode(normalizeBarcode(i.getProductBarcode())));
    }

    public static void normalizeOrderSearchForm(OrderSearchForm form) {
        if (form == null) return;
        if (StringUtils.hasText(form.getOrderReferenceId())) {
            form.setOrderReferenceId(form.getOrderReferenceId().trim().toUpperCase());
        }
        if (StringUtils.hasText(form.getStatus())) {
            form.setStatus(form.getStatus().trim().toUpperCase());
        }
    }

    public static void normalizeInventoryUpdateForm(InventoryUpdateForm form) {
        if (form == null) return;
        if (form.getBarcode() != null) {
            form.setBarcode(form.getBarcode().trim().toUpperCase());
        }
    }

    public static String normalizeOrderReferenceId(String ref) {
        if (!StringUtils.hasText(ref)) return null;
        return ref.trim().toUpperCase();
    }

    public static void normalizeOrderUpdateForm(OrderUpdateForm form) {
        if (form == null) return;
        form.setOrderReferenceId(normalizeOrderReferenceId(form.getOrderReferenceId()));
        if (form.getItems() != null) {
            form.getItems().forEach(i ->
                    i.setProductBarcode(normalizeBarcode(i.getProductBarcode()))
            );
        }
    }

    public static void normalizeSignupForm(SignupForm form) {
        if (form == null) return;
        form.setEmail(normalizeEmail(form.getEmail()));
    }

    public static void normalizeLoginForm(LoginForm form) {
        if (form == null) return;
        form.setEmail(normalizeEmail(form.getEmail()));
    }

    public static String normalizeBearerTokenFromHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader)) return null;

        String h = authHeader.trim();
        if (!h.startsWith("Bearer ")) return null;

        String token = h.substring(7).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    public static void normalizeCreateOperatorForm(CreateOperatorForm form) {
        if (form == null) return;
        form.setEmail(normalizeEmail(form.getEmail()));
    }
}
