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
        normalizeOrderCreate(form);
        validateOrderCreate(form);
        OrderPojo order = OrderHelper.convertCreateFormToEntity(form);
        return OrderHelper.convertToData(orderFlow.create(order));
    }

    public OrderData updateOrder(String orderReferenceId, OrderCreateForm form) throws ApiException {
        String ref = normalizeAndValidateRef(orderReferenceId);
        normalizeOrderCreate(form);
        validateOrderCreate(form);
        OrderPojo updated = OrderHelper.convertCreateFormToEntity(form);
        return OrderHelper.convertToData(orderFlow.update(ref, updated));
    }

    public OrderData cancelOrder(String orderReferenceId) throws ApiException {
        String ref = normalizeAndValidateRef(orderReferenceId);
        return OrderHelper.convertToData(orderFlow.cancel(ref));
    }

    public OrderData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        String ref = normalizeAndValidateRef(orderReferenceId);
        return OrderHelper.convertToData(orderFlow.getByRef(ref));
    }

    public Page<OrderData> getAllOrders(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<OrderPojo> page =
                orderFlow.search(null, null, null, null, form.getPage(), form.getSize());
        return toDataPage(page);
    }

    public Page<OrderData> filterOrders(OrderFilterForm form) throws ApiException {
        String rawStatus = form != null ? form.getStatus() : null;
        validateStatusFilterRaw(rawStatus);
        normalizeOrderFilter(form);
        ValidationUtil.validateOrderFilterForm(form);
        TimeRange range = computeTimeRange(form.getTimeframe());
        Page<OrderPojo> page = searchOrders(form, range);
        return toDataPage(page);
    }

    // -------------------- Normalization + Validation helpers --------------------

    private void normalizeOrderCreate(OrderCreateForm form) {
        NormalizationUtil.normalizeOrderCreateForm(form);
    }

    private void normalizeOrderFilter(OrderFilterForm form) {
        NormalizationUtil.normalizeOrderFilterForm(form);
    }

    private String normalizeAndValidateRef(String orderReferenceId) throws ApiException {
        String normalized = normalizeOrderRef(orderReferenceId);
        validateOrderRef(normalized);
        return normalized;
    }

    private String normalizeOrderRef(String value) {
        if (!StringUtils.hasText(value)) return value;
        return value.trim().toUpperCase();
    }

    private void validateOrderRef(String value) throws ApiException {
        if (!StringUtils.hasText(value)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        if (value.length() > 50) {
            throw new ApiException("Order reference id too long");
        }
    }

    private void validateOrderCreate(OrderCreateForm form) throws ApiException {
        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }

        Set<String> seenBarcodes = new HashSet<>();
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

            String barcode = item.getProductBarcode(); // normalized already
            if (!seenBarcodes.add(barcode)) {
                throw new ApiException("Duplicate product barcode in order: " + barcode);
            }
        }
    }
    private void validateStatusFilterRaw(String status) throws ApiException {
        if (!StringUtils.hasText(status)) return;

        String s = status.trim().toUpperCase();

        // treat ALL as invalid for API filter
        if ("ALL".equals(s)) {
            throw new ApiException("Invalid order status filter: " + status);
        }

        try {
            OrderStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid order status filter: " + status);
        }
    }

    private record TimeRange(ZonedDateTime from, ZonedDateTime to) {}

    private TimeRange computeTimeRange(OrderTimeframe timeframe) {
        ZonedDateTime to = ZonedDateTime.now();
        ZonedDateTime from = computeFromTime(timeframe, to);
        return new TimeRange(from, to);
    }

    private ZonedDateTime computeFromTime(OrderTimeframe timeframe, ZonedDateTime now) {
        if (timeframe == null) timeframe = OrderTimeframe.LAST_MONTH;

        return switch (timeframe) {
            case LAST_DAY -> now.minusDays(1);
            case LAST_WEEK -> now.minusWeeks(1);
            case LAST_MONTH -> now.minusMonths(1);
        };
    }

    private Page<OrderPojo> searchOrders(OrderFilterForm form, TimeRange range) {
        return orderFlow.search(
                form.getOrderReferenceId(),
                form.getStatus(),
                range.from(),
                range.to(),
                form.getPage(),
                form.getSize()
        );
    }

    private Page<OrderData> toDataPage(Page<OrderPojo> page) {
        List<OrderData> data = page.getContent()
                .stream()
                .map(OrderHelper::convertToData)
                .collect(Collectors.toList());
        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }
}
