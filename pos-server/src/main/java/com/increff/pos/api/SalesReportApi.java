package com.increff.pos.api;

import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.exception.ApiException;

import java.time.LocalDate;
import java.util.List;

public interface SalesReportApi {
    List<SalesReportRowPojo> getDailyReport(LocalDate date, String clientEmail, ReportRowType type) throws ApiException;
    List<SalesReportRowPojo> getRangeReport(LocalDate start, LocalDate end, String clientEmail, ReportRowType type) throws ApiException;

    void generateAndStoreDaily(LocalDate date) throws ApiException;
    boolean existsForDate(LocalDate date);
}
