package com.increff.pos.model.data;

import lombok.Data;

@Data
public class OrderItemData {
    private String productBarcode;
    private Integer quantity;
    private Double sellingPrice;
}
