package com.increff.pos.helper;

import com.increff.pos.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

public class TsvHelper {
    public static List<String[]> decode(String base64) throws ApiException {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String content = new String(decoded, StandardCharsets.UTF_8);

            String[] lines = content.split("\n");
            List<String[]> rows = new ArrayList<>();

            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    rows.add(lines[i].trim().split("\t", -1));
                }
            }
            return rows;
        } catch (Exception e) {
            throw new ApiException("Invalid TSV file");
        }
    }

    public static String encodeResult(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("barcode\tstatus\tmessage\n");
        for (String[] row : rows) {
            sb.append(String.join("\t", row)).append("\n");
        }
        return Base64.getEncoder()
                .encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
