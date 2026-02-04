package com.increff.pos.helper;

import com.increff.pos.model.exception.ApiException;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TsvHelper {

    public static Pair<Map<String, Integer>, List<String[]>> parse(String base64FileContent) throws ApiException {
        String fileContent = decodeBase64ToText(base64FileContent);
        String normalizedContent = normalizeNewlines(fileContent);

        String[] lines = normalizedContent.split("\n", -1);

        int headerLineIndex = findHeaderLineIndex(lines);
        String[] headerColumns = lines[headerLineIndex].split("\t", -1);

        Map<String, Integer> headerIndexByName = buildHeaderIndex(headerColumns);
        List<String[]> dataRows = readDataRows(lines, headerLineIndex + 1);

        if (dataRows.isEmpty()) {
            throw new ApiException("File is empty");
        }

        return Pair.of(headerIndexByName, dataRows);
    }

    public static String encodeResult(List<String[]> rows) {
        StringBuilder output = new StringBuilder();
        output.append("barcode\tstatus\tcomment\n");

        if (rows == null || rows.isEmpty()) {
            return Base64.getEncoder()
                    .encodeToString(output.toString().getBytes(StandardCharsets.UTF_8));
        }

        for (String[] row : rows) {
            output.append(String.join("\t", row)).append("\n");
        }

        return Base64.getEncoder()
                .encodeToString(output.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBase64ToText(String base64) throws ApiException {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException("Invalid TSV file");
        }
    }

    private static String normalizeNewlines(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static int findHeaderLineIndex(String[] lines) throws ApiException {
        if (lines == null || lines.length == 0) {
            throw new ApiException("TSV must contain header and data");
        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && !lines[i].isBlank()) {
                return i;
            }
        }

        throw new ApiException("TSV must contain header and data");
    }

    private static Map<String, Integer> buildHeaderIndex(String[] headerColumns) {
        Map<String, Integer> headerIndexByName = new HashMap<>();
        if (headerColumns == null) {
            return headerIndexByName;
        }

        for (int i = 0; i < headerColumns.length; i++) {
            String rawHeader = headerColumns[i];
            String headerKey = rawHeader == null ? "" : rawHeader.trim().toLowerCase();

            if (!headerKey.isEmpty()) {
                headerIndexByName.put(headerKey, i);
            }
        }

        return headerIndexByName;
    }

    private static List<String[]> readDataRows(String[] lines, int startIndex) {
        List<String[]> rows = new ArrayList<>();
        if (lines == null) {
            return rows;
        }

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }
            rows.add(line.split("\t", -1));
        }

        return rows;
    }
}
