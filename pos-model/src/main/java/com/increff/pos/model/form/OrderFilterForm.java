package com.increff.pos.model.form;

import com.increff.pos.model.constants.OrderTimeframe;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderFilterForm {

    @Size(max = 50, message = "Order reference id filter too long")
    private String orderReferenceId;

    @Size(max = 30, message = "Status filter too long")
    private String status;

    private OrderTimeframe timeframe;

    @Min(value = 0, message = "Page cannot be negative")
    private int page = 0;

    @Min(value = 1, message = "Invalid page size")
    @Max(value = 100, message = "Invalid page size")
    private int size = 10;
}
