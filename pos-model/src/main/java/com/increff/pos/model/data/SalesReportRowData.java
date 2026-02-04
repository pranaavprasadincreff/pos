package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesReportRowData {
    private String clientEmail;
    private String productBarcode;

    private long ordersCount;
    private long itemsCount;
    private double totalRevenue;
}
