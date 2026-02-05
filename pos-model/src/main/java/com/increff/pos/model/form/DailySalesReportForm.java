package com.increff.pos.model.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DailySalesReportForm {
    private LocalDate date;

    @Size(max = 40, message = "clientEmail too long")
    @Email(message = "Invalid clientEmail")
    private String clientEmail;
}
