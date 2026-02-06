package com.increff.pos.dto;

import com.increff.pos.api.SalesReportApi;
import com.increff.pos.helper.SalesReportHelper;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportResponseData;
import com.increff.pos.model.data.SalesReportRowData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.DailySalesReportForm;
import com.increff.pos.model.form.RangeSalesReportForm;
import com.increff.pos.util.NormalizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SalesReportDto {
    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");
    private static final int MAX_RANGE_DAYS = 92;

    @Autowired
    private SalesReportApi salesReportApi;

    public SalesReportResponseData getDailyReport(DailySalesReportForm requestForm) throws ApiException {
        DailySalesReportForm validatedRequest = normalizeAndValidateDailyRequest(requestForm);
        LocalDate reportDate = validatedRequest.getDate();
        String clientEmail = validatedRequest.getClientEmail();
        ReportRowType reportRowType = computeReportRowType(clientEmail);
        List<SalesReportRowData> reportRows =
                salesReportApi.getDailyReport(reportDate, clientEmail, reportRowType);

        return SalesReportHelper.toResponseData(
                "DAILY",
                reportDate,
                reportDate,
                clientEmail,
                reportRowType,
                reportRows
        );
    }

    public SalesReportResponseData getRangeReport(RangeSalesReportForm requestForm) throws ApiException {
        RangeSalesReportForm validatedRequest = normalizeAndValidateRangeRequest(requestForm);
        LocalDate startDate = validatedRequest.getStartDate();
        LocalDate endDate = validatedRequest.getEndDate();
        String clientEmail = validatedRequest.getClientEmail();
        ReportRowType reportRowType = computeReportRowType(clientEmail);
        List<SalesReportRowData> reportRows =
                salesReportApi.getRangeReport(startDate, endDate, clientEmail, reportRowType);

        return SalesReportHelper.toResponseData(
                "RANGE",
                startDate,
                endDate,
                clientEmail,
                reportRowType,
                reportRows
        );
    }

    public void generateAndStoreDailyNested(LocalDate reportDate) {
        if (shouldSkipReportGeneration(reportDate)) {
            return;
        }
        if (salesReportApi.existsDailyNested(reportDate)) {
            return;
        }
        generateDailyNestedReport(reportDate);
    }

    private DailySalesReportForm normalizeAndValidateDailyRequest(DailySalesReportForm requestForm) throws ApiException {
        DailySalesReportForm normalizedForm = normalizeDailyRequest(requestForm);
        applyDefaultReportDate(normalizedForm);
        validateDailyRequest(normalizedForm);
        return normalizedForm;
    }

    private DailySalesReportForm normalizeDailyRequest(DailySalesReportForm requestForm) {
        DailySalesReportForm normalizedForm;
        if (requestForm == null) {
            normalizedForm = new DailySalesReportForm();
        } else {
            normalizedForm = requestForm;
        }

        normalizedForm.setClientEmail(normalizeClientEmail(normalizedForm.getClientEmail()));
        return normalizedForm;
    }

    private void applyDefaultReportDate(DailySalesReportForm form) {
        if (form.getDate() != null) {
            return;
        }
        form.setDate(getYesterdayIstDate());
    }

    private void validateDailyRequest(DailySalesReportForm form) throws ApiException {
        if (form.getDate() == null) {
            throw new ApiException("reportDate is required for daily report");
        }
        ensureDateNotInFuture(form.getDate(), "Daily report date cannot be in the future");
    }

    private RangeSalesReportForm normalizeAndValidateRangeRequest(RangeSalesReportForm requestForm) throws ApiException {
        RangeSalesReportForm normalizedForm = normalizeRangeRequest(requestForm);
        validateRangeRequest(normalizedForm);
        return normalizedForm;
    }

    private RangeSalesReportForm normalizeRangeRequest(RangeSalesReportForm requestForm) throws ApiException {
        if (requestForm == null) {
            throw new ApiException("Invalid request");
        }
        requestForm.setClientEmail(normalizeClientEmail(requestForm.getClientEmail()));
        return requestForm;
    }

    private void validateRangeRequest(RangeSalesReportForm form) throws ApiException {
        if (form.getStartDate().isAfter(form.getEndDate())) {
            throw new ApiException("startDate cannot be after endDate");
        }

        ensureDateNotInFuture(form.getStartDate(), "startDate cannot be in the future");
        ensureDateNotInFuture(form.getEndDate(), "endDate cannot be in the future");

        long inclusiveDays = ChronoUnit.DAYS.between(form.getStartDate(), form.getEndDate()) + 1;
        if (inclusiveDays > MAX_RANGE_DAYS) {
            throw new ApiException("Date range too large (max 92 days)");
        }
    }

    private void ensureDateNotInFuture(LocalDate date, String message) throws ApiException {
        LocalDate todayInIst = LocalDate.now(IST_TIMEZONE);
        if (date.isAfter(todayInIst)) {
            throw new ApiException(message);
        }
    }

    private boolean shouldSkipReportGeneration(LocalDate reportDate) {
        if (reportDate == null) {
            return true;
        }
        LocalDate todayInIst = LocalDate.now(IST_TIMEZONE);
        if (reportDate.isAfter(todayInIst)) {
            return true;
        }
        return false;
    }

    private void generateDailyNestedReport(LocalDate reportDate) {
        try {
            salesReportApi.generateAndStoreDailyNested(reportDate);
        } catch (Exception ignored) {
            // intentionally ignored
        }
    }

    private LocalDate getYesterdayIstDate() {
        return LocalDate.now(IST_TIMEZONE).minusDays(1);
    }

    private String normalizeClientEmail(String clientEmail) {
        if (!StringUtils.hasText(clientEmail)) {
            return null;
        }
        return NormalizationUtil.normalizeEmail(clientEmail);
    }

    private ReportRowType computeReportRowType(String clientEmail) {
        if (StringUtils.hasText(clientEmail)) {
            return ReportRowType.PRODUCT;
        }
        return ReportRowType.CLIENT;
    }
}
