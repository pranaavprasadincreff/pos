package com.increff.pos.api;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.data.domain.Page;

public interface OrderApi {
    OrderPojo createOrder(OrderPojo order) throws ApiException;
    OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException;
    void markOrderInvoiced(String orderReferenceId) throws ApiException;
    OrderPojo updateOrder(String orderReferenceId, OrderPojo updatedOrder) throws ApiException;
    OrderPojo cancelOrder(String orderReferenceId) throws ApiException;
    Page<OrderPojo> getAllOrders(int page, int size);
}
