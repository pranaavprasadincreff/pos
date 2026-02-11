package com.increff.pos.db;

import com.increff.pos.db.subdocument.OrderItemPojo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "orders")
@CompoundIndexes({
        @CompoundIndex(name = "idx_orders_orderReferenceId", def = "{'orderReferenceId': 1}", unique = true),
        @CompoundIndex(name = "idx_orders_status_createdAt", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_orders_createdAt", def = "{'createdAt': -1}")
})
public class OrderPojo extends AbstractPojo {
    private String orderReferenceId;
    private String status;
    private List<OrderItemPojo> orderItems;
}
