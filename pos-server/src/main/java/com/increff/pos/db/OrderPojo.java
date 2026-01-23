package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Document(collection = "orders")
public class OrderPojo extends AbstractPojo {
    @Indexed(unique = true)
    private String orderReferenceId;
    private ZonedDateTime orderTime;
    private String status;
    private List<OrderItemPojo> orderItems;
}
