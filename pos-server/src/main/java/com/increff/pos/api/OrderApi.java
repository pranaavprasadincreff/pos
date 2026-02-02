package com.increff.pos.api;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;

public interface OrderApi {
    OrderPojo createOrder(OrderPojo order) throws ApiException;
    OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException;
    OrderPojo updateOrder(OrderPojo order) throws ApiException;
    boolean orderReferenceIdExists(String orderReferenceId);
    Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    );
}
