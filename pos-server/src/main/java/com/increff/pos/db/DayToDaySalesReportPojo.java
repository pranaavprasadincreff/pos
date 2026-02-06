package com.increff.pos.db;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "sales_report_daily")
public class DayToDaySalesReportPojo extends AbstractPojo {

    private LocalDate date;
    private List<ClientBlock> clients;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClientBlock {
        private String clientEmail;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;
        private List<ProductBlock> products;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductBlock {
        // TODO use productId
        private String productBarcode;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;
    }
}
