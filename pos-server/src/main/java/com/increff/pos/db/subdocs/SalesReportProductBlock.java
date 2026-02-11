package com.increff.pos.db.subdocs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SalesReportProductBlock {
    private String productBarcode;
    private Long ordersCount;
    private Long itemsCount;
    private Double totalRevenue;
}
