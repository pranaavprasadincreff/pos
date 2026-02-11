package com.increff.pos.db.subdocs;

import com.increff.pos.db.AbstractPojo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemPojo extends AbstractPojo {
    private String productId;
    private Integer orderedQuantity;
    private Double sellingPrice;
}
