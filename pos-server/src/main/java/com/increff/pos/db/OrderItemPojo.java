package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class OrderItemPojo extends AbstractPojo{
    @Indexed
    // TODO product id when storing in db
    private String productBarcode;
    private Integer orderedQuantity;
    private Double sellingPrice;
}
