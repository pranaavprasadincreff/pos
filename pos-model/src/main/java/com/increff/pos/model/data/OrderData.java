package com.increff.pos.model.data;

import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class OrderData {
    private String orderReferenceId;
    private ZonedDateTime orderTime;
    private String status;
    private List<OrderItemData> items;
}
