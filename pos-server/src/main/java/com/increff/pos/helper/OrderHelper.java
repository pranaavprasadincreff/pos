package com.increff.pos.helper;

import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;

import java.util.List;
import java.util.stream.Collectors;

public class OrderHelper {

    public static OrderPojo buildOrderForCreate(List<Integer> quantities, List<Double> sellingPrices) {
        OrderPojo order = new OrderPojo();
        order.setOrderItems(buildOrderItems(quantities, sellingPrices));
        return order;
    }

    private static List<OrderItemPojo> buildOrderItems(List<Integer> quantities, List<Double> sellingPrices) {
        return java.util.stream.IntStream.range(0, quantities.size())
                .mapToObj(i -> buildOrderItem(quantities.get(i), sellingPrices.get(i)))
                .collect(Collectors.toList());
    }

    private static OrderItemPojo buildOrderItem(Integer quantity, Double sellingPrice) {
        OrderItemPojo item = new OrderItemPojo();
        item.setOrderedQuantity(quantity);
        item.setSellingPrice(sellingPrice);
        return item;
    }

    public static OrderData buildOrderData(OrderPojo order, List<OrderItemData> items) {
        OrderData data = new OrderData();
        data.setOrderReferenceId(order.getOrderReferenceId());
        data.setOrderTime(order.getOrderTime());
        data.setStatus(order.getStatus());
        data.setItems(items);
        return data;
    }
}
