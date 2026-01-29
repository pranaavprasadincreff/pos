package com.increff.pos.api;

import com.increff.pos.dao.OrderDao;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class OrderApiImpl implements OrderApi {
    private final OrderDao orderDao;
    private final InventoryApi inventoryApi;

    public OrderApiImpl(OrderDao orderDao, InventoryApi inventoryApi) {
        this.orderDao = orderDao;
        this.inventoryApi = inventoryApi;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo createOrder(OrderPojo order) throws ApiException {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
        boolean fulfillable = inventoryApi.tryDeductInventoryForOrder(order.getOrderItems());
        order.setStatus(
                fulfillable ? OrderStatus.FULFILLABLE.name() : OrderStatus.UNFULFILLABLE.name()
        );
        return orderDao.save(order);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo updateOrder(String ref, OrderPojo updated) throws ApiException {
        OrderPojo existing = getByOrderReferenceId(ref);
        validateEditable(existing);
        if (isFulfillable(existing)) {
            inventoryApi.restoreInventoryForOrder(existing.getOrderItems());
        }
        boolean fulfillable = inventoryApi.tryDeductInventoryForOrder(updated.getOrderItems());
        existing.setOrderItems(updated.getOrderItems());
        existing.setStatus(
                fulfillable
                        ? OrderStatus.FULFILLABLE.name()
                        : OrderStatus.UNFULFILLABLE.name()
        );

        return orderDao.save(existing);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancelOrder(String ref) throws ApiException {
        OrderPojo order = getByOrderReferenceId(ref);
        validateEditable(order);
        if (isFulfillable(order)) {
            inventoryApi.restoreInventoryForOrder(order.getOrderItems());
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        return orderDao.save(order);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void markOrderInvoiced(String ref) throws ApiException {
        OrderPojo order = getByOrderReferenceId(ref);
        if (isInvoiced(order)) {
            throw new ApiException("Order already invoiced");
        }
        if (!isFulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
        order.setStatus(OrderStatus.INVOICED.name());
        orderDao.save(order);
    }

    @Override
    public OrderPojo getByOrderReferenceId(String ref) throws ApiException {
        OrderPojo order = orderDao.findByOrderReferenceId(ref);
        if (order == null) {
            throw new ApiException("Order not found: " + ref);
        }
        return order;
    }

    @Override
    public Page<OrderPojo> getAllOrders(int page, int size) {
        return orderDao.findAll(
                PageRequest.of(page, size, Sort.by("orderTime").descending())
        );
    }

    private void validateEditable(OrderPojo order) throws ApiException {
        if (isCancelled(order) || isInvoiced(order)) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private boolean isFulfillable(OrderPojo o) {
        return OrderStatus.FULFILLABLE.name().equals(o.getStatus());
    }

    private boolean isCancelled(OrderPojo o) {
        return OrderStatus.CANCELLED.name().equals(o.getStatus());
    }

    private boolean isInvoiced(OrderPojo o) {
        return OrderStatus.INVOICED.name().equals(o.getStatus());
    }

    private String generateUniqueOrderReferenceId() {
        String ref;
        int attempts = 0;
        do {
            if (attempts > 2) {
                throw new RuntimeException("Unable to generate unique order ID after 10 attempts");
            }
            String part1 = String.format("%04d", (int) (Math.random() * 10000));
            String part2 = String.format("%04d", (int) (Math.random() * 10000));
            ref = "ORD-" + part1 + "-" + part2;
            attempts++;
        } while (orderDao.findByOrderReferenceId(ref) != null);
        return ref;
    }
}
