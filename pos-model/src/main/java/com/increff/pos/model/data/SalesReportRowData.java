package com.increff.pos.model.data;

import lombok.Data;

@Data
public class SalesReportRowData {
    private String clientEmail;
    private String productBarcode;

    private long ordersCount;
    private long itemsCount;
    private double totalRevenue;
}
