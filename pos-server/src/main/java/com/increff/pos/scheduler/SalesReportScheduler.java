package com.increff.pos.scheduler;

import com.increff.pos.api.SalesReportApi;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SalesReportScheduler {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private SalesReportApi salesReportApi;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void generateDailySalesReport() throws ApiException {
        LocalDate reportDate = LocalDate.now(IST).minusDays(1);
        salesReportApi.generateAndStoreDaily(reportDate);
    }
}
