package com.increff.pos.util;

import com.increff.pos.model.form.*;
import org.springframework.util.StringUtils;

import java.util.List;

public class NormalizationUtil {
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

    public static void normalizeClientFilterForm(ClientFilterForm form) {
        form.setName(normalizeName(form.getName()));
        form.setEmail(normalizeEmail(form.getEmail()));
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) return email;
        return email.trim().toLowerCase();
    }

    public static String normalizeName(String name) {
        if (!StringUtils.hasText(name)) return name;
        return name.trim().toLowerCase();
    }

    public static String normalizeBarcode(String value) {
        if (!StringUtils.hasText(value)) return value;
        return value.trim().toUpperCase();
    }

    public static void normalizeProductFilterForm(ProductFilterForm form) {
        if (form == null) return;
        form.setBarcode(normalizeBarcode(form.getBarcode()));
        form.setName(normalizeName(form.getName()));
        form.setClient(normalizeName(form.getClient()));
    }

    public static void normalizeProductForm(ProductForm form) {
        form.setBarcode(NormalizationUtil.normalizeBarcode(form.getBarcode()));
        form.setClientEmail(NormalizationUtil.normalizeEmail(form.getClientEmail()));
        form.setName(NormalizationUtil.normalizeName(form.getName()));
    }

    public static void normalizeProductUpdateForm(ProductUpdateForm form) {
        form.setOldBarcode(NormalizationUtil.normalizeBarcode(form.getOldBarcode()));
        form.setNewBarcode(NormalizationUtil.normalizeBarcode(form.getNewBarcode()));
        form.setClientEmail(NormalizationUtil.normalizeEmail(form.getClientEmail()));
        form.setName(NormalizationUtil.normalizeName(form.getName()));
    }

    public static void normalizeBulkProductRows(List<String[]> rows) {
        for (String[] r : rows) {
            r[0] = normalizeBarcode(r[0]);   // barcode
            r[1] = normalizeEmail(r[1]);     // clientEmail
            r[2] = normalizeName(r[2]);      // name
            if (r.length == 5) r[4] = r[4].trim(); // imageUrl
        }
    }

    public static void normalizeBulkInventoryRows(List<String[]> rows) {
        for (String[] r : rows) {
            r[0] = normalizeBarcode(r[0]); // barcode
        }
    }
}
