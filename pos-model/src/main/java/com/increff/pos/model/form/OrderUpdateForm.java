package com.increff.pos.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderUpdateForm {

    @NotBlank(message = "Order reference id is required")
    @Size(max = 50, message = "Order reference id too long")
    private String orderReferenceId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderCreateItemForm> items;
}
