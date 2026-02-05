package com.increff.pos.controller;

import com.increff.pos.dto.SalesReportDto;
import com.increff.pos.model.data.SalesReportResponseData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.DailySalesReportForm;
import com.increff.pos.model.form.RangeSalesReportForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sales Report")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/reports/sales")
public class SalesReportController {
    @Autowired
    private SalesReportDto salesReportDto;

    @Operation(summary = "Daily sales report")
    @PostMapping("/daily")
    public SalesReportResponseData daily(@Valid @RequestBody(required = false) DailySalesReportForm form) throws ApiException {
        return salesReportDto.getDailyReport(form);
    }

    @Operation(summary = "Date-range sales report")
    @PostMapping("/range")
    public SalesReportResponseData range(@Valid @RequestBody RangeSalesReportForm form) throws ApiException {
        return salesReportDto.getRangeReport(form);
    }
}
