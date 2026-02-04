package com.increff.pos.helper;

import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportResponseData;
import com.increff.pos.model.data.SalesReportRowData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesReportHelper {

    private SalesReportHelper() {}

    public static SalesReportResponseData toResponseData(
            String reportKind,
            LocalDate start,
            LocalDate end,
            String clientEmail,
            ReportRowType rowType,
            List<SalesReportRowPojo> rows
    ) {
        SalesReportResponseData data = new SalesReportResponseData();
        data.setReportKind(reportKind);
        data.setStartDate(start);
        data.setEndDate(end);
        data.setClientEmail(clientEmail);
        data.setRowType(rowType.name());
        data.setGeneratedAt(ZonedDateTime.now());
        data.setRows(mapRows(rows));
        return data;
    }

    private static List<SalesReportRowData> mapRows(List<SalesReportRowPojo> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        List<SalesReportRowData> out = new ArrayList<>();
        for (SalesReportRowPojo row : rows) {
            out.add(toRowData(row));
        }
        return out;
    }

    private static SalesReportRowData toRowData(SalesReportRowPojo p) {
        SalesReportRowData d = new SalesReportRowData();
        d.setClientEmail(p.getClientEmail());
        d.setProductBarcode(p.getProductBarcode());
        d.setOrdersCount(p.getOrdersCount());
        d.setItemsCount(p.getItemsCount());
        d.setTotalRevenue(p.getTotalRevenue());
        return d;
    }
}
