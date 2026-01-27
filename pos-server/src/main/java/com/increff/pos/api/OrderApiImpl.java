package com.increff.pos.api;

import com.increff.pos.dao.OrderDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderApiImpl implements OrderApi {
    private static final Logger logger = LoggerFactory.getLogger(OrderApiImpl.class);
    private final OrderDao orderDao;
    private final ProductApi productApi;
    private final InventoryApi inventoryApi;

    public OrderApiImpl(
            OrderDao orderDao,
            ProductApi productApi,
            InventoryApi inventoryApi
    ) {
        this.orderDao = orderDao;
        this.productApi = productApi;
        this.inventoryApi = inventoryApi;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public OrderPojo createOrder(OrderPojo order) throws ApiException {
        logger.info("Creating order with {} items", order.getOrderItems().size());
        initializeOrder(order);
        deductInventoryForOrder(order);
        persistOrderWithUniqueReference(order);
        return order;
    }

    @Override
    public OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException {
        return findOrderOrThrow(orderReferenceId);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public void markOrderInvoiced(String orderReferenceId) throws ApiException {
        OrderPojo order = findOrderOrThrow(orderReferenceId);
        if (!isInvoiced(order)) {
            setOrderInvoiced(order);
        }
    }

    @Override
    public Page<OrderPojo> getAllOrders(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "orderTime")
        );
        return orderDao.findAll(pageRequest);
    }

    private void initializeOrder(OrderPojo order) {
        order.setOrderTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
        order.setStatus("CREATED");
    }

    private void deductInventoryForOrder(OrderPojo order) throws ApiException {
        for (OrderItemPojo item : order.getOrderItems()) {
            deductInventoryForItem(item);
        }
    }

    private void persistOrderWithUniqueReference(OrderPojo order) throws ApiException {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                order.setOrderReferenceId(generateOrderReferenceId());
                orderDao.save(order);
                return;
            } catch (DuplicateKeyException e) {
                logger.warn("Order referenceId collision, retrying...");
            }
        }
        throw new ApiException("Unable to generate unique order reference id");
    }

    private String generateOrderReferenceId() {
        String uuid = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + uuid.substring(0, 4)
                + "-" + uuid.substring(4, 8);
    }

    private OrderPojo findOrderOrThrow(String orderReferenceId) throws ApiException {
        OrderPojo order = orderDao.findByOrderReferenceId(orderReferenceId);
        if (order == null) {
            throw new ApiException("Order not present: " + orderReferenceId);
        }
        return order;
    }

    private boolean isInvoiced(OrderPojo order) {
        return "INVOICED".equals(order.getStatus());
    }

    private void setOrderInvoiced(OrderPojo order) {
        order.setStatus("INVOICED");
        orderDao.save(order);
    }

    private void deductInventoryForItem(OrderItemPojo item) throws ApiException {
        String barcode = item.getProductBarcode();
        try {
            ProductPojo product = productApi.getProductByBarcode(barcode);
            InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
            inventoryApi.deductInventory(
                    inventory,
                    item.getOrderedQuantity()
            );
        } catch (ApiException e) {
            throw new ApiException(
                    "Insufficient inventory for product barcode: " + barcode
            );
        }
    }
}
