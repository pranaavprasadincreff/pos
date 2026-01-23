package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
public class OrderItemPojo extends AbstractPojo{
    @Indexed
    private String productBarcode;
    private Integer orderedQuantity;
    private Double sellingPrice;
}
