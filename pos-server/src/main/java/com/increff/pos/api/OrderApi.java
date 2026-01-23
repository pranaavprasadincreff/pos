package com.increff.pos.api;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.exception.ApiException;

public interface OrderApi {
    OrderPojo createOrder(OrderPojo order) throws ApiException;
    OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException;
    void markOrderInvoiced(String orderReferenceId) throws ApiException;
}
