package com.increff.pos.model.form;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class OrderCreateForm {
    private List<OrderCreateItemForm> items;
}
