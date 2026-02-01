package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class OrderFlow {
    @Autowired
    private OrderApi orderApi;
    @Autowired
    private InventoryApi inventoryApi;

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo create(OrderPojo order) throws ApiException {
        populateNewOrderFields(order);
        boolean fulfillable = tryDeductInventory(order);
        setStatusByFulfillability(order, fulfillable);
        return orderApi.createOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo update(String ref, OrderPojo updated) throws ApiException {
        OrderPojo existing = orderApi.getByOrderReferenceId(ref);
        validateEditable(existing);
        restoreInventoryIfNeeded(existing);
        boolean fulfillable = tryDeductInventory(updated);
        applyUpdate(existing, updated, fulfillable);
        return orderApi.updateOrder(existing);
    }

    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo cancel(String ref) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(ref);
        validateEditable(order);
        restoreInventoryIfNeeded(order);
        order.setStatus(OrderStatus.CANCELLED.name());
        return orderApi.updateOrder(order);
    }

    @Transactional(rollbackFor = ApiException.class)
    public void markInvoiced(String ref) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(ref);
        validateInvoicingAllowed(order);
        order.setStatus(OrderStatus.INVOICED.name());
        orderApi.updateOrder(order);
    }

    public OrderPojo getByRef(String ref) throws ApiException {
        return orderApi.getByOrderReferenceId(ref);
    }

    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    ) {
        return orderApi.search(refContains, status, fromTime, toTime, page, size);
    }

    private void populateNewOrderFields(OrderPojo order) {
        order.setOrderReferenceId(generateUniqueOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now());
    }

    private boolean tryDeductInventory(OrderPojo order) throws ApiException {
        return inventoryApi.tryDeductInventoryForOrder(order.getOrderItems());
    }

    private void setStatusByFulfillability(OrderPojo order, boolean fulfillable) {
        order.setStatus(fulfillable
                ? OrderStatus.FULFILLABLE.name()
                : OrderStatus.UNFULFILLABLE.name());
    }

    private void restoreInventoryIfNeeded(OrderPojo order) throws ApiException {
        if (isUnfulfillable(order)) return;
        inventoryApi.restoreInventoryForOrder(order.getOrderItems());
    }

    private void applyUpdate(OrderPojo existing, OrderPojo updated, boolean fulfillable) {
        existing.setOrderItems(updated.getOrderItems());
        setStatusByFulfillability(existing, fulfillable);
    }

    private void validateEditable(OrderPojo order) throws ApiException {
        if (isCancelled(order) || isInvoiced(order)) {
            throw new ApiException("Order cannot be modified in current state");
        }
    }

    private void validateInvoicingAllowed(OrderPojo order) throws ApiException {
        if (isInvoiced(order)) {
            throw new ApiException("Order already invoiced");
        }
        if (isUnfulfillable(order)) {
            throw new ApiException("Only fulfillable orders can be invoiced");
        }
    }

    private boolean isUnfulfillable(OrderPojo o) {
        return !OrderStatus.FULFILLABLE.name().equals(o.getStatus());
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
            attempts++;
            validateRefGenerationAttempts(attempts);
            ref = randomRef();
        } while (orderApi.orderReferenceIdExists(ref));

        return ref;
    }

    private void validateRefGenerationAttempts(int attempts) {
        if (attempts > 10) {
            throw new RuntimeException("Unable to generate unique order ID after 10 attempts");
        }
    }

    private String randomRef() {
        String part1 = String.format("%04d", (int) (Math.random() * 10000));
        String part2 = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD-" + part1 + "-" + part2;
    }
}
