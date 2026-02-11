package com.increff.pos.dto;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.subdocument.OrderItemPojo;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.helper.OrderHelper;
import com.increff.pos.model.constants.OrderTimeframe;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderCreateItemForm;
import com.increff.pos.model.form.OrderSearchForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.util.FormValidationUtil;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Component
public class OrderDto {

    @Autowired
    private OrderFlow orderFlow;

    public OrderData createOrder(OrderCreateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderCreateForm(form);
        FormValidationUtil.validate(form);

        List<String> barcodesInOrder = extractBarcodes(form.getItems());
        Map<String, ProductPojo> productByBarcode = orderFlow.getProductsByBarcode(barcodesInOrder);

        OrderPojo orderToCreate = OrderHelper.buildOrderPojo(form.getItems(), productByBarcode);
        orderToCreate.setOrderReferenceId(OrderHelper.generateOrderReferenceId());

        OrderPojo created = orderFlow.create(orderToCreate);
        return buildOrderData(created);
    }

    public OrderData updateOrder(OrderUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderUpdateForm(form);
        FormValidationUtil.validate(form);

        // keep this validation because it's not a bean-validation constraint on form fields in many implementations
        ValidationUtil.validateOrderReferenceId(form.getOrderReferenceId());

        List<String> barcodesInOrder = extractBarcodes(form.getItems());
        Map<String, ProductPojo> productByBarcode = orderFlow.getProductsByBarcode(barcodesInOrder);

        OrderPojo updateRequest = OrderHelper.buildOrderPojo(form.getItems(), productByBarcode);
        updateRequest.setOrderReferenceId(form.getOrderReferenceId());

        OrderPojo updated = orderFlow.update(updateRequest);
        return buildOrderData(updated);
    }

    public OrderData cancelOrder(String orderReferenceId) throws ApiException {
        String normalized = NormalizationUtil.normalizeOrderReferenceId(orderReferenceId);
        ValidationUtil.validateOrderReferenceId(normalized);

        OrderPojo cancelled = orderFlow.cancel(normalized);
        return buildOrderData(cancelled);
    }

    public OrderData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        String normalized = NormalizationUtil.normalizeOrderReferenceId(orderReferenceId);
        ValidationUtil.validateOrderReferenceId(normalized);

        OrderPojo order = orderFlow.getByRef(normalized);
        return buildOrderData(order);
    }

    public Page<OrderData> searchOrders(OrderSearchForm form) throws ApiException {
        NormalizationUtil.normalizeOrderSearchForm(form);
        FormValidationUtil.validate(form);

        TimeRange timeRange = computeTimeRange(form.getTimeframe());
        Page<OrderPojo> orders = orderFlow.search(
                form.getOrderReferenceId(),
                form.getStatus(),
                timeRange.from(),
                timeRange.to(),
                form.getPage(),
                form.getSize()
        );

        List<OrderData> data = orders.getContent().stream()
                .map(this::buildOrderData)
                .toList();

        return new PageImpl<>(data, orders.getPageable(), orders.getTotalElements());
    }

    private OrderData buildOrderData(OrderPojo order) {
        List<String> productIdsInOrder = order.getOrderItems().stream()
                .map(OrderItemPojo::getProductId)
                .distinct()
                .toList();

        Map<String, ProductPojo> productById = orderFlow.getProductsByIds(productIdsInOrder);

        List<OrderItemData> itemData = order.getOrderItems().stream()
                .map(item -> OrderHelper.buildOrderItemData(item, productById.get(item.getProductId())))
                .toList();

        return OrderHelper.buildOrderData(order, itemData);
    }

    private static List<String> extractBarcodes(List<OrderCreateItemForm> items) {
        return items.stream()
                .map(OrderCreateItemForm::getProductBarcode)
                .toList();
    }

    private TimeRange computeTimeRange(OrderTimeframe timeframe) {
        ZonedDateTime to = ZonedDateTime.now();
        ZonedDateTime from = computeFromTime(timeframe, to);
        return new TimeRange(from, to);
    }

    private ZonedDateTime computeFromTime(OrderTimeframe timeframe, ZonedDateTime now) {
        OrderTimeframe effective = timeframe == null ? OrderTimeframe.LAST_MONTH : timeframe;

        return switch (effective) {
            case LAST_DAY -> now.minusDays(1);
            case LAST_WEEK -> now.minusWeeks(1);
            case LAST_MONTH -> now.minusMonths(1);
        };
    }

    private record TimeRange(ZonedDateTime from, ZonedDateTime to) {}
}
