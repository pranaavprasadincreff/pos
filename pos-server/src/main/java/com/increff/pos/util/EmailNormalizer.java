package com.increff.pos.util;

import java.text.Normalizer;
import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        // Unicode normalization (important in enterprise systems)
        email = Normalizer.normalize(email, Normalizer.Form.NFKC);

        return email;
    }
}
