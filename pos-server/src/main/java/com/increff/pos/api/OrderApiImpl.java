package com.increff.pos.api;

import com.increff.pos.dao.OrderDao;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class OrderApiImpl implements OrderApi {

    @Autowired
    private OrderDao orderDao;

    @Override
    public OrderPojo createOrder(OrderPojo order) {
        return orderDao.save(order);
    }

    @Override
    public OrderPojo updateOrder(OrderPojo order) {
        return orderDao.save(order);
    }

    @Override
    public boolean orderReferenceIdExists(String orderReferenceId) {
        return orderDao.findByOrderReferenceId(orderReferenceId) != null;
    }

    @Override
    public OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException {
        OrderPojo order = orderDao.findByOrderReferenceId(orderReferenceId);
        if (order == null) {
            throw new ApiException("Order not found: " + orderReferenceId);
        }
        return order;
    }

    @Override
    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    ) {
        return orderDao.search(refContains, status, fromTime, toTime, page, size);
    }
}
