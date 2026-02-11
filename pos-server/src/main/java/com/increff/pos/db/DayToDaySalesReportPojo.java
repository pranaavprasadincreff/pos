package com.increff.pos.db;

import com.increff.pos.db.subdocs.SalesReportClientBlock;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "sales_report_daily")
@CompoundIndexes({
        @CompoundIndex(name = "idx_sales_report_daily_date", def = "{'date': 1}", unique = true)
})
public class DayToDaySalesReportPojo extends AbstractPojo {
    private LocalDate date;
    private List<SalesReportClientBlock> clients;
}
