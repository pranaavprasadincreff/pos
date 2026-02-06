package com.increff.pos.api;

import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportRowData;

import java.time.LocalDate;
import java.util.List;

public interface SalesReportApi {
    List<SalesReportRowData> getDailyReport(LocalDate reportDate, String clientEmail, ReportRowType rowType);
    List<SalesReportRowData> getRangeReport(LocalDate startDate, LocalDate endDate, String clientEmail, ReportRowType rowType);
    void generateAndStoreDailyNested(LocalDate reportDate);
    boolean existsDailyNested(LocalDate reportDate);
}
