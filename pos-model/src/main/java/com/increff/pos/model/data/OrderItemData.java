package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemData {
    private String productBarcode;
    private Integer quantity;
    private Double sellingPrice;
}
