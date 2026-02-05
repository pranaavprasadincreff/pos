package com.increff.pos.helper;

import com.increff.pos.db.DayToDaySalesReportPojo;
import com.increff.pos.db.SalesReportRowPojo;
import org.springframework.util.StringUtils;

import java.util.*;

public class SalesReportRangeAccumulator {

    private SalesReportRangeAccumulator() {}

    public static List<SalesReportRowPojo> accumulateClientRows(List<DayToDaySalesReportPojo> reportDocuments) {
        Map<String, SalesReportRowPojo> rowsByClientEmail = new HashMap<>();

        for (DayToDaySalesReportPojo reportDocument : reportDocuments) {
            if (reportDocument == null || reportDocument.getClients() == null) continue;

            for (DayToDaySalesReportPojo.ClientBlock clientBlock : reportDocument.getClients()) {
                if (clientBlock == null || !StringUtils.hasText(clientBlock.getClientEmail())) continue;

                SalesReportRowPojo accumulatorRow = rowsByClientEmail.computeIfAbsent(
                        clientBlock.getClientEmail(),
                        SalesReportRangeAccumulator::createClientAccumulatorRow
                );

                accumulatorRow.setOrdersCount(accumulatorRow.getOrdersCount() + nullSafeLong(clientBlock.getOrdersCount()));
                accumulatorRow.setItemsCount(accumulatorRow.getItemsCount() + nullSafeLong(clientBlock.getItemsCount()));
                accumulatorRow.setTotalRevenue(accumulatorRow.getTotalRevenue() + nullSafeDouble(clientBlock.getTotalRevenue()));
            }
        }

        return sortRowsByRevenueDescending(rowsByClientEmail.values());
    }

    public static List<SalesReportRowPojo> accumulateProductRows(List<DayToDaySalesReportPojo> reportDocuments, String clientEmail) {
        if (!StringUtils.hasText(clientEmail)) {
            return List.of();
        }

        Map<String, SalesReportRowPojo> rowsByBarcode = new HashMap<>();

        for (DayToDaySalesReportPojo reportDocument : reportDocuments) {
            if (reportDocument == null || reportDocument.getClients() == null) continue;

            DayToDaySalesReportPojo.ClientBlock clientBlock = findClientBlock(reportDocument.getClients(), clientEmail);
            if (clientBlock == null || clientBlock.getProducts() == null) continue;

            for (DayToDaySalesReportPojo.ProductBlock productBlock : clientBlock.getProducts()) {
                if (productBlock == null || !StringUtils.hasText(productBlock.getProductBarcode())) continue;

                SalesReportRowPojo accumulatorRow = rowsByBarcode.computeIfAbsent(
                        productBlock.getProductBarcode(),
                        barcode -> createProductAccumulatorRow(clientEmail, barcode)
                );

                accumulatorRow.setOrdersCount(accumulatorRow.getOrdersCount() + nullSafeLong(productBlock.getOrdersCount()));
                accumulatorRow.setItemsCount(accumulatorRow.getItemsCount() + nullSafeLong(productBlock.getItemsCount()));
                accumulatorRow.setTotalRevenue(accumulatorRow.getTotalRevenue() + nullSafeDouble(productBlock.getTotalRevenue()));
            }
        }

        return sortRowsByRevenueDescending(rowsByBarcode.values());
    }

    private static SalesReportRowPojo createClientAccumulatorRow(String clientEmail) {
        SalesReportRowPojo row = new SalesReportRowPojo();
        row.setClientEmail(clientEmail);
        row.setProductBarcode(null);
        row.setOrdersCount(0);
        row.setItemsCount(0);
        row.setTotalRevenue(0.0);
        return row;
    }

    private static SalesReportRowPojo createProductAccumulatorRow(String clientEmail, String productBarcode) {
        SalesReportRowPojo row = new SalesReportRowPojo();
        row.setClientEmail(clientEmail);
        row.setProductBarcode(productBarcode);
        row.setOrdersCount(0);
        row.setItemsCount(0);
        row.setTotalRevenue(0.0);
        return row;
    }

    private static DayToDaySalesReportPojo.ClientBlock findClientBlock(List<DayToDaySalesReportPojo.ClientBlock> clients, String clientEmail) {
        for (DayToDaySalesReportPojo.ClientBlock clientBlock : clients) {
            if (clientBlock == null) continue;
            if (clientEmail.equals(clientBlock.getClientEmail())) return clientBlock;
        }
        return null;
    }

    private static List<SalesReportRowPojo> sortRowsByRevenueDescending(Collection<SalesReportRowPojo> rows) {
        List<SalesReportRowPojo> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return sortedRows;
    }

    private static long nullSafeLong(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
    }

    private static double nullSafeDouble(Double value) {
        if (value == null) {
            return 0.0;
        }
        return value;
    }
}
