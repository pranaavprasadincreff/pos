package com.increff.pos.model.form;

import lombok.Data;

@Data
public class InventoryUpdateForm {
    private String productId;
    private Integer quantity;
}
