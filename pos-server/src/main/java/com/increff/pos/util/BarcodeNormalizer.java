package com.increff.pos.util;

import java.text.Normalizer;

public final class BarcodeNormalizer {

    private BarcodeNormalizer() {}

    public static String normalize(String rawBarcode) {
        if (rawBarcode == null) {
            return null;
        }
        String barcode = rawBarcode.trim().toUpperCase();
        return Normalizer.normalize(barcode, Normalizer.Form.NFKC);
    }
}
