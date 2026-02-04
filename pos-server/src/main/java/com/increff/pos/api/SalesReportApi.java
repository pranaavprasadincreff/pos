package com.increff.pos.api;

import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;

import java.time.LocalDate;
import java.util.List;

public interface SalesReportApi {

    List<SalesReportRowPojo> getDailyReport(LocalDate reportDate, String clientEmail, ReportRowType rowType);
    List<SalesReportRowPojo> getRangeReport(LocalDate startDate, LocalDate endDate, String clientEmail, ReportRowType rowType);
    void generateAndStoreDailyNested(LocalDate reportDate);
    boolean existsDailyNested(LocalDate reportDate);
}
