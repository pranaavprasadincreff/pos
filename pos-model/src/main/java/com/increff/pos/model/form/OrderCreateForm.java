package com.increff.pos.model.form;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateForm {
    private List<OrderCreateItemForm> items;
}
