package com.increff.pos.dto;

import com.increff.pos.api.SalesReportApi;
import com.increff.pos.db.SalesReportRowPojo;
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
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SalesReportDto {
    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Autowired
    private SalesReportApi salesReportApi;

    public SalesReportResponseData getDailyReport(SalesReportForm requestForm) throws ApiException {
        SalesReportForm validatedForm = normalizeAndValidateDailyRequest(requestForm);

        LocalDate reportDate = validatedForm.getStartDate();
        String clientEmail = validatedForm.getClientEmail();
        ReportRowType rowType = computeRowType(clientEmail);
        List<SalesReportRowPojo> dailyReport =
                salesReportApi.getDailyReport(reportDate, clientEmail, rowType);

        return SalesReportHelper.toResponseData(
                "DAILY",
                reportDate,
                reportDate,
                clientEmail,
                rowType,
                dailyReport
        );
    }

    public SalesReportResponseData getRangeReport(SalesReportForm requestForm) throws ApiException {
        SalesReportForm validatedForm = normalizeAndValidateRangeRequest(requestForm);

        LocalDate startDate = validatedForm.getStartDate();
        LocalDate endDate = validatedForm.getEndDate();
        String clientEmail = validatedForm.getClientEmail();
        ReportRowType rowType = computeRowType(clientEmail);
        List<SalesReportRowPojo> rangeReport =
                salesReportApi.getRangeReport(startDate, endDate, clientEmail, rowType);

        return SalesReportHelper.toResponseData(
                "RANGE",
                startDate,
                endDate,
                clientEmail,
                rowType,
                rangeReport
        );
    }

    public void generateAndStoreDailyNested(LocalDate reportDate) {
        if (isNotEligibleReportDate(reportDate)) return;
        generateAndStoreNestedDailyReportSafely(reportDate);
    }

    public void generateAndStoreDailyNestedIfMissing(LocalDate reportDate) {
        if (isNotEligibleReportDate(reportDate)) return;
        if (salesReportApi.existsDailyNested(reportDate)) return;
        generateAndStoreNestedDailyReportSafely(reportDate);
    }

    // -------------------- Validation + Normalization --------------------

    private SalesReportForm normalizeAndValidateDailyRequest(SalesReportForm requestForm) throws ApiException {
        SalesReportForm normalizedForm = normalizeRequestForm(requestForm);
        validateDailyRequest(normalizedForm);
        return normalizedForm;
    }

    private SalesReportForm normalizeAndValidateRangeRequest(SalesReportForm requestForm) throws ApiException {
        SalesReportForm normalizedForm = normalizeRequestForm(requestForm);
        validateRangeRequest(normalizedForm);
        return normalizedForm;
    }

    private SalesReportForm normalizeRequestForm(SalesReportForm requestForm) {
        SalesReportForm normalizedForm = (requestForm == null) ? new SalesReportForm() : requestForm;

        normalizedForm.setClientEmail(normalizeEmail(normalizedForm.getClientEmail()));
        applyDefaultDailyDateIfMissing(normalizedForm);

        return normalizedForm;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) return null;
        return email.trim().toLowerCase();
    }

    private void applyDefaultDailyDateIfMissing(SalesReportForm form) {
        if (form.getStartDate() == null && form.getEndDate() == null) {
            form.setStartDate(getYesterdayIstDate());
        }
    }

    private void validateDailyRequest(SalesReportForm form) throws ApiException {
        validateFormPresent(form);
        validateDailyDates(form);
        validateClientEmail(form.getClientEmail());
        validateNotFutureDate(form.getStartDate());
    }

    private void validateRangeRequest(SalesReportForm form) throws ApiException {
        validateFormPresent(form);
        validateRangeDates(form);
        validateClientEmail(form.getClientEmail());
        validateRangeWithinLimit(form);
    }

    private void validateFormPresent(SalesReportForm form) throws ApiException {
        if (form == null) throw new ApiException("Invalid request");
    }

    private void validateDailyDates(SalesReportForm form) throws ApiException {
        if (form.getStartDate() == null) {
            throw new ApiException("startDate is required for daily report");
        }

        if (form.getEndDate() != null && !form.getEndDate().equals(form.getStartDate())) {
            throw new ApiException("For daily report, endDate must be null or equal to startDate");
        }
    }

    private void validateRangeDates(SalesReportForm form) throws ApiException {
        if (form.getStartDate() == null || form.getEndDate() == null) {
            throw new ApiException("startDate and endDate are required for range report");
        }

        if (form.getStartDate().isAfter(form.getEndDate())) {
            throw new ApiException("startDate cannot be after endDate");
        }
    }

    private void validateNotFutureDate(LocalDate date) throws ApiException {
        LocalDate todayIst = LocalDate.now(IST_TIMEZONE);
        if (date.isAfter(todayIst)) {
            throw new ApiException("Daily report date cannot be in the future");
        }
    }

    private void validateClientEmail(String email) throws ApiException {
        if (!StringUtils.hasText(email)) return;
        if (email.length() > 120) throw new ApiException("clientEmail too long");
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new ApiException("Invalid clientEmail");
    }

    private void validateRangeWithinLimit(SalesReportForm form) throws ApiException {
        long days = java.time.temporal.ChronoUnit.DAYS.between(form.getStartDate(), form.getEndDate()) + 1;
        if (days > 92) throw new ApiException("Date range too large (max 92 days)");
    }

    // -------------------- Nested daily generation helpers --------------------

    private boolean isNotEligibleReportDate(LocalDate reportDate) {
        if (reportDate == null) return true;
        LocalDate todayIst = LocalDate.now(IST_TIMEZONE);
        return reportDate.isAfter(todayIst);
    }

    private LocalDate getYesterdayIstDate() {
        return LocalDate.now(IST_TIMEZONE).minusDays(1);
    }

    private void generateAndStoreNestedDailyReportSafely(LocalDate reportDate) {
        try {
            salesReportApi.generateAndStoreDailyNested(reportDate);
        } catch (Exception ignored) {
            // intentionally ignored
        }
    }

    private ReportRowType computeRowType(String clientEmail) {
        return StringUtils.hasText(clientEmail) ? ReportRowType.PRODUCT : ReportRowType.CLIENT;
    }
}
