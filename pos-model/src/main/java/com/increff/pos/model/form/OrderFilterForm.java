package com.increff.pos.model.form;

import com.increff.pos.model.constants.OrderTimeframe;
import lombok.Data;

@Data
public class OrderFilterForm {
    private String orderReferenceId;
    private String status;
    private OrderTimeframe timeframe;

    private int page;
    private int size;
}
