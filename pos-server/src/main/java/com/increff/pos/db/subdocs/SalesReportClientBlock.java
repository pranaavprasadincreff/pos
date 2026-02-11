package com.increff.pos.db.subdocs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SalesReportClientBlock {
    private String clientEmail;
    private Long ordersCount;
    private Long itemsCount;
    private Double totalRevenue;
    private List<SalesReportProductBlock> products;
}
