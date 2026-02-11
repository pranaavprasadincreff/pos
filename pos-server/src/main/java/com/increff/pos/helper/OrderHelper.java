package com.increff.pos.helper;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.subdocs.OrderItemPojo;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderCreateItemForm;

import java.util.List;
import java.util.Map;

public class OrderHelper {

    private static final Object LOCK = new Object();
    private static int seq = 0;
    private static long lastEpochMillis = -1;

    public static String generateOrderReferenceId() {
        long now = System.currentTimeMillis();
        int localSeq;

        synchronized (LOCK) {
            if (now != lastEpochMillis) {
                lastEpochMillis = now;
                seq = 0;
            } else {
                seq++;
            }
            localSeq = seq;
        }

        if (localSeq == 0) {
            return "ORD-" + now;
        }
        return "ORD-%d-%d".formatted(now, localSeq);
    }

    // ---------------- build order ----------------

    public static OrderPojo buildOrderPojo(
            List<OrderCreateItemForm> itemForms,
            Map<String, ProductPojo> productByBarcodeMap
    ) {
        OrderPojo orderPojo = new OrderPojo();
        orderPojo.setOrderItems(buildOrderItems(itemForms, productByBarcodeMap));
        return orderPojo;
    }

    private static List<OrderItemPojo> buildOrderItems(
            List<OrderCreateItemForm> itemForms,
            Map<String, ProductPojo> productByBarcodeMap
    ) {
        return itemForms.stream()
                .map(form -> buildOrderItem(form, productByBarcodeMap))
                .toList();
    }

    private static OrderItemPojo buildOrderItem(
            OrderCreateItemForm form,
            Map<String, ProductPojo> productByBarcodeMap
    ) {
        ProductPojo product = productByBarcodeMap.get(form.getProductBarcode());

        OrderItemPojo itemPojo = new OrderItemPojo();
        itemPojo.setProductId(product.getId());
        itemPojo.setOrderedQuantity(form.getQuantity());
        itemPojo.setSellingPrice(form.getSellingPrice());
        return itemPojo;
    }

    // ---------------- response mapping ----------------

    public static OrderItemData buildOrderItemData(OrderItemPojo itemPojo, ProductPojo product) {
        OrderItemData data = new OrderItemData();
        data.setQuantity(itemPojo.getOrderedQuantity());
        data.setSellingPrice(itemPojo.getSellingPrice());
        data.setProductBarcode(product == null ? null : product.getBarcode());
        return data;
    }

    public static OrderData buildOrderData(OrderPojo order, List<OrderItemData> items) {
        OrderData data = new OrderData();
        data.setOrderReferenceId(order.getOrderReferenceId());
        data.setStatus(order.getStatus());
        data.setCreatedAt(order.getCreatedAt());
        data.setItems(items);
        return data;
    }
}
