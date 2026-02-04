package com.increff.pos.dto;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.helper.OrderHelper;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.OrderTimeframe;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderFilterForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderDto {

    @Autowired
    private OrderFlow orderFlow;

    public OrderData createOrder(OrderCreateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderCreateForm(form);
        validateOrderCreateForm(form);

        OrderPojo orderToCreate = OrderHelper.convertCreateFormToEntity(form);
        OrderPojo createdOrder = orderFlow.create(orderToCreate);

        return OrderHelper.convertToData(createdOrder);
    }

    public OrderData updateOrder(String orderReferenceId, OrderCreateForm form) throws ApiException {
        String normalizedOrderReferenceId = normalizeAndValidateOrderReferenceId(orderReferenceId);

        NormalizationUtil.normalizeOrderCreateForm(form);
        validateOrderCreateForm(form);

        OrderPojo updatedOrderRequest = OrderHelper.convertCreateFormToEntity(form);
        OrderPojo updatedOrder = orderFlow.update(normalizedOrderReferenceId, updatedOrderRequest);

        return OrderHelper.convertToData(updatedOrder);
    }

    public OrderData cancelOrder(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = normalizeAndValidateOrderReferenceId(orderReferenceId);

        OrderPojo cancelledOrder = orderFlow.cancel(normalizedOrderReferenceId);

        return OrderHelper.convertToData(cancelledOrder);
    }

    public OrderData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        String normalizedOrderReferenceId = normalizeAndValidateOrderReferenceId(orderReferenceId);

        OrderPojo order = orderFlow.getByRef(normalizedOrderReferenceId);

        return OrderHelper.convertToData(order);
    }

    public Page<OrderData> getAllOrders(PageForm form) throws ApiException {
        Page<OrderData> page = searchAllOrders(form);
        return page;
    }

    public Page<OrderData> filterOrders(OrderFilterForm form) throws ApiException {
        validateRawStatusFilter(form);
        NormalizationUtil.normalizeOrderFilterForm(form);
        ValidationUtil.validateOrderFilterForm(form);

        TimeRange timeRange = computeTimeRange(form.getTimeframe());
        Page<OrderPojo> orders = searchOrders(form, timeRange);

        return convertToOrderDataPage(orders);
    }

    // -------------------- Private helpers --------------------

    private Page<OrderData> searchAllOrders(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);

        Page<OrderPojo> orders = orderFlow.search(
                null,
                null,
                null,
                null,
                form.getPage(),
                form.getSize()
        );

        Page<OrderData> page = convertToOrderDataPage(orders);
        return page;
    }

    private void validateRawStatusFilter(OrderFilterForm form) throws ApiException {
        String rawStatus = form != null ? form.getStatus() : null;

        if (!StringUtils.hasText(rawStatus)) {
            return;
        }

        String normalized = rawStatus.trim().toUpperCase();

        if ("ALL".equals(normalized)) {
            throw new ApiException("Invalid order status filter: " + rawStatus);
        }

        try {
            OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid order status filter: " + rawStatus);
        }
    }

    private String normalizeAndValidateOrderReferenceId(String orderReferenceId) throws ApiException {
        String normalized = normalizeOrderReferenceId(orderReferenceId);
        validateOrderReferenceId(normalized);
        return normalized;
    }

    private String normalizeOrderReferenceId(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.trim().toUpperCase();
    }

    private void validateOrderReferenceId(String value) throws ApiException {
        if (!StringUtils.hasText(value)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        if (value.length() > 50) {
            throw new ApiException("Order reference id too long");
        }
    }

    private void validateOrderCreateForm(OrderCreateForm form) throws ApiException {
        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }

        Set<String> uniqueBarcodes = new HashSet<>();

        for (var item : form.getItems()) {
            if (!StringUtils.hasText(item.getProductBarcode())) {
                throw new ApiException("Product barcode cannot be empty");
            }
            if (item.getProductBarcode().length() > 40) {
                throw new ApiException("Barcode too long");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ApiException("Invalid quantity");
            }
            if (item.getQuantity() > 1000) {
                throw new ApiException("Quantity cannot exceed 1000");
            }
            if (item.getSellingPrice() == null || item.getSellingPrice() <= 0) {
                throw new ApiException("Invalid selling price");
            }

            String normalizedBarcode = item.getProductBarcode();
            boolean firstTime = uniqueBarcodes.add(normalizedBarcode);
            if (!firstTime) {
                throw new ApiException("Duplicate product barcode in order: " + normalizedBarcode);
            }
        }
    }

    private TimeRange computeTimeRange(OrderTimeframe timeframe) {
        ZonedDateTime toTime = ZonedDateTime.now();
        ZonedDateTime fromTime = computeFromTime(timeframe, toTime);
        return new TimeRange(fromTime, toTime);
    }

    private ZonedDateTime computeFromTime(OrderTimeframe timeframe, ZonedDateTime now) {
        OrderTimeframe effectiveTimeframe = timeframe == null ? OrderTimeframe.LAST_MONTH : timeframe;

        return switch (effectiveTimeframe) {
            case LAST_DAY -> now.minusDays(1);
            case LAST_WEEK -> now.minusWeeks(1);
            case LAST_MONTH -> now.minusMonths(1);
        };
    }

    private Page<OrderPojo> searchOrders(OrderFilterForm form, TimeRange timeRange) {
        return orderFlow.search(
                form.getOrderReferenceId(),
                form.getStatus(),
                timeRange.from(),
                timeRange.to(),
                form.getPage(),
                form.getSize()
        );
    }

    private Page<OrderData> convertToOrderDataPage(Page<OrderPojo> orders) {
        List<OrderData> data = orders.getContent()
                .stream()
                .map(OrderHelper::convertToData)
                .collect(Collectors.toList());

        return new PageImpl<>(data, orders.getPageable(), orders.getTotalElements());
    }

    private record TimeRange(ZonedDateTime from, ZonedDateTime to) {
    }
}
