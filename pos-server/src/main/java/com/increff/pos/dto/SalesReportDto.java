package com.increff.pos.dto;

import com.increff.pos.api.SalesReportApi;
import com.increff.pos.helper.SalesReportHelper;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportResponseData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.SalesReportForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

@Component
public class SalesReportDto {
    @Autowired
    private SalesReportApi salesReportApi;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public SalesReportResponseData getDailyReport(SalesReportForm form) throws ApiException {
        SalesReportForm f = normalize(form);
        validateDaily(f);

        LocalDate date = f.getStartDate();
        String clientEmail = f.getClientEmail();
        ReportRowType type = computeRowType(clientEmail);

        return SalesReportHelper.toResponseData(
                "DAILY",
                date,
                date,
                clientEmail,
                type,
                salesReportApi.getDailyReport(date, clientEmail, type)
        );
    }

    public SalesReportResponseData getRangeReport(SalesReportForm form) throws ApiException {
        SalesReportForm f = normalize(form);
        validateRange(f);

        LocalDate start = f.getStartDate();
        LocalDate end = f.getEndDate();
        String clientEmail = f.getClientEmail();
        ReportRowType type = computeRowType(clientEmail);

        return SalesReportHelper.toResponseData(
                "RANGE",
                start,
                end,
                clientEmail,
                type,
                salesReportApi.getRangeReport(start, end, clientEmail, type)
        );
    }

    // -------------------- Normalization + Validation helpers --------------------

    private SalesReportForm normalize(SalesReportForm form) {
        SalesReportForm f = (form == null) ? new SalesReportForm() : form;

        if (StringUtils.hasText(f.getClientEmail())) {
            f.setClientEmail(f.getClientEmail().trim().toLowerCase());
        } else {
            f.setClientEmail(null);
        }

        // daily convenience default (yesterday IST) if caller sends empty body
        if (f.getStartDate() == null && f.getEndDate() == null) {
            f.setStartDate(LocalDate.now(IST).minusDays(1));
        }

        return f;
    }

    private void validateDaily(SalesReportForm form) throws ApiException {
        if (form == null) throw new ApiException("Invalid request");

        if (form.getStartDate() == null) {
            throw new ApiException("startDate is required for daily report");
        }

        if (form.getEndDate() != null && !form.getEndDate().equals(form.getStartDate())) {
            throw new ApiException("For daily report, endDate must be null or equal to startDate");
        }

        validateClientEmail(form.getClientEmail());

        LocalDate today = LocalDate.now(IST);
        if (form.getStartDate().isAfter(today)) {
            throw new ApiException("Daily report date cannot be in the future");
        }
    }

    private void validateRange(SalesReportForm form) throws ApiException {
        if (form == null) throw new ApiException("Invalid request");

        if (form.getStartDate() == null || form.getEndDate() == null) {
            throw new ApiException("startDate and endDate are required for range report");
        }

        if (form.getStartDate().isAfter(form.getEndDate())) {
            throw new ApiException("startDate cannot be after endDate");
        }

        validateClientEmail(form.getClientEmail());

        long days = java.time.temporal.ChronoUnit.DAYS.between(form.getStartDate(), form.getEndDate()) + 1;
        if (days > 92) {
            throw new ApiException("Date range too large (max 92 days)");
        }
    }

    private void validateClientEmail(String email) throws ApiException {
        if (!StringUtils.hasText(email)) return;
        if (email.length() > 120) throw new ApiException("clientEmail too long");
        if (!EMAIL.matcher(email).matches()) throw new ApiException("Invalid clientEmail");
    }

    private ReportRowType computeRowType(String clientEmail) {
        return StringUtils.hasText(clientEmail) ? ReportRowType.PRODUCT : ReportRowType.CLIENT;
    }
}
