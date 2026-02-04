package com.increff.pos.scheduler;

import com.increff.pos.api.SalesReportApi;
import com.increff.pos.dto.SalesReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SalesReportScheduler {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private SalesReportDto salesReportDto;

    @Autowired
    private SalesReportApi salesReportApi;

    @PostConstruct
    public void catchUpOnStartup() {
        LocalDate reportDate = LocalDate.now(IST).minusDays(1);

        if (!salesReportApi.existsForDate(reportDate)) {
            try {
                salesReportDto.generateAndStoreDaily(reportDate);
            } catch (Exception e) {
                // intentionally ignored
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void generateDailySalesReport() {
        LocalDate reportDate = LocalDate.now(IST).minusDays(1);
        try {
            salesReportDto.generateAndStoreDaily(reportDate);
        } catch (Exception e) {
            // intentionally ignored
        }
    }
}
