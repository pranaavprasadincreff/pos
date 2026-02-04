package com.increff.pos.scheduler;

import com.increff.pos.dto.SalesReportDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SalesReportScheduler {
    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private SalesReportDto salesReportDto;

    @PostConstruct
    public void generateYesterdayReportOnStartupIfMissing() {
        LocalDate reportDate = getYesterdayIstDate();
        salesReportDto.generateAndStoreDailyNestedIfMissing(reportDate);
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void generateYesterdayReportAtMidnight() {
        LocalDate reportDate = getYesterdayIstDate();
        salesReportDto.generateAndStoreDailyNested(reportDate);
    }

    private LocalDate getYesterdayIstDate() {
        return LocalDate.now(IST_TIMEZONE).minusDays(1);
    }
}
