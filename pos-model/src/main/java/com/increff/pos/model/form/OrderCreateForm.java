package com.increff.pos.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateForm {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderCreateItemForm> items;
}
