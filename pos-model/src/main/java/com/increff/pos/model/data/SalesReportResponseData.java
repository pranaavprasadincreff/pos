package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class SalesReportResponseData {
    private String reportKind;
    private LocalDate startDate;
    private LocalDate endDate;

    private String clientEmail;
    private String rowType;

    private ZonedDateTime generatedAt;
    private List<SalesReportRowData> rows;
}
