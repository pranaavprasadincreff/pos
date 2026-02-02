package com.increff.pos.helper;

import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportResponseData;
import com.increff.pos.model.data.SalesReportRowData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class SalesReportHelper {

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
        data.setRows(rows == null ? List.of() : rows.stream().map(SalesReportHelper::toRowData).toList());
        return data;
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
