package com.increff.pos.model.form;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesReportForm {
    private LocalDate startDate;
    private LocalDate endDate;
    private String clientEmail;
}
