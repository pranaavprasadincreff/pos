package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "daily_sales_report")
public class SalesReportAggregatePojo extends AbstractPojo {
    @Indexed
    private LocalDate date;
    @Indexed
    private String type;
    @Indexed
    private String clientEmail;
    @Indexed
    private String productBarcode;

    private long ordersCount;
    private long itemsCount;
    private double totalRevenue;
}
