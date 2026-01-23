package com.increff.pos.helper;

import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderCreateItemForm;

import java.util.stream.Collectors;

public class OrderHelper {
    public static OrderPojo convertCreateFormToEntity(OrderCreateForm form) {
        OrderPojo order = new OrderPojo();
        order.setOrderItems(
                form.getItems()
                        .stream()
                        .map(OrderHelper::convertItemFormToEntity)
                        .collect(Collectors.toList())
        );
        return order;
    }

    private static OrderItemPojo convertItemFormToEntity(OrderCreateItemForm form) {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductBarcode(form.getProductBarcode());
        item.setOrderedQuantity(form.getQuantity());
        item.setSellingPrice(form.getSellingPrice());
        return item;
    }

    public static OrderData convertToData(OrderPojo order) {
        OrderData data = new OrderData();
        data.setOrderReferenceId(order.getOrderReferenceId());
        data.setOrderTime(order.getOrderTime());
        data.setStatus(order.getStatus());
        data.setItems(
                order.getOrderItems()
                        .stream()
                        .map(OrderHelper::convertItemToData)
                        .collect(Collectors.toList())
        );
        return data;
    }

    private static OrderItemData convertItemToData(OrderItemPojo item) {
        OrderItemData data = new OrderItemData();
        data.setProductBarcode(item.getProductBarcode());
        data.setQuantity(item.getOrderedQuantity());
        data.setSellingPrice(item.getSellingPrice());
        return data;
    }
}
