package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesReportRowPojo {
    private String clientEmail;
    private String productBarcode;

    private long ordersCount;
    private long itemsCount;
    private double totalRevenue;
}
