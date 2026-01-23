package com.increff.pos.api;

import com.increff.pos.dao.OrderDao;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        order.setOrderReferenceId(generateOrderReferenceId());
        order.setOrderTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
        order.setStatus("created");
        deductInventory(order.getOrderItems());
        return orderDao.save(order);
    }

    @Override
    public OrderPojo getByOrderReferenceId(String orderReferenceId) throws ApiException {
        OrderPojo order = orderDao.findByOrderReferenceId(orderReferenceId);
        if (order == null) {
            throw new ApiException(
                    "Order not found with referenceId: " + orderReferenceId
            );
        }
        return order;
    }

    private void deductInventory(List<OrderItemPojo> items) throws ApiException {
        for (OrderItemPojo item : items) {
            ProductPojo product = productApi.getProductByBarcode(item.getProductBarcode());
            InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
            int available = inventory.getQuantity();
            int required = item.getOrderedQuantity();
            if (available < required) {
                throw new ApiException(
                        "Insufficient inventory for barcode: "
                                + item.getProductBarcode()
                                + ", available: " + available
                                + ", required: " + required
                );
            }
            inventory.setQuantity(available - required);
            inventoryApi.updateInventory(inventory);
        }
    }

    private String generateOrderReferenceId() {
        String uuid = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + uuid.substring(0, 4)
                + "-" + uuid.substring(4, 8);
    }
}
