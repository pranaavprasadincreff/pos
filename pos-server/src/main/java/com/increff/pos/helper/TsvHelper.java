package com.increff.pos.helper;

import com.increff.pos.model.exception.ApiException;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

public class TsvHelper {

    public static Pair<Map<String, Integer>, List<String[]>> parse(String base64) throws ApiException {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String content = new String(decoded, StandardCharsets.UTF_8);

            // Normalize newlines
            content = content.replace("\r\n", "\n").replace("\r", "\n");

            String[] lines = content.split("\n", -1);

            // Find first non-empty line as header
            int headerLineIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && !lines[i].isBlank()) {
                    headerLineIndex = i;
                    break;
                }
            }
            if (headerLineIndex == -1) throw new ApiException("TSV must contain header and data");

            String headerLine = lines[headerLineIndex];
            String[] headerArr = headerLine.split("\t", -1);

            Map<String, Integer> headers = new HashMap<>();
            for (int i = 0; i < headerArr.length; i++) {
                String key = headerArr[i] == null ? "" : headerArr[i].trim().toLowerCase();
                if (!key.isEmpty()) {
                    headers.put(key, i);
                }
            }

            List<String[]> rows = new ArrayList<>();
            for (int i = headerLineIndex + 1; i < lines.length; i++) {
                String line = lines[i];
                if (line == null || line.isBlank()) continue;
                rows.add(line.split("\t", -1));
            }

            if (rows.isEmpty()) throw new ApiException("File is empty");
            return Pair.of(headers, rows);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Invalid TSV file");
        }
    }

    public static String encodeResult(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("barcode\tstatus\tcomment\n");
        for (String[] r : rows) {
            sb.append(String.join("\t", r)).append("\n");
        }
        return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
