package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class OrderData {
    private String orderReferenceId;
    private String status;
    private ZonedDateTime createdAt;
    private List<OrderItemData> items;

}
