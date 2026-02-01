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
        normalizeAndValidateCreate(form);
        OrderPojo order = OrderHelper.convertCreateFormToEntity(form);
        return OrderHelper.convertToData(orderFlow.create(order));
    }

    public OrderData updateOrder(String orderReferenceId, OrderCreateForm form) throws ApiException {
        validateRef(orderReferenceId);
        normalizeAndValidateCreate(form);
        OrderPojo updated = OrderHelper.convertCreateFormToEntity(form);
        return OrderHelper.convertToData(orderFlow.update(orderReferenceId, updated));
    }

    public OrderData cancelOrder(String orderReferenceId) throws ApiException {
        validateRef(orderReferenceId);
        return OrderHelper.convertToData(orderFlow.cancel(orderReferenceId));
    }

    public OrderData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        validateRef(orderReferenceId);
        return OrderHelper.convertToData(orderFlow.getByRef(orderReferenceId));
    }

    public Page<OrderData> getAllOrders(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<OrderPojo> page = orderFlow.search(null, null, null, null, form.getPage(), form.getSize());
        return toDataPage(page);
    }

    public Page<OrderData> filterOrders(OrderFilterForm form) throws ApiException {
        normalizeAndValidateFilterForm(form);
        TimeRange range = computeTimeRange(form.getTimeframe());
        Page<OrderPojo> page = searchOrders(form, range);
        return toDataPage(page);
    }

    private void normalizeAndValidateFilterForm(OrderFilterForm form) throws ApiException {
        NormalizationUtil.normalizeOrderFilterForm(form);
        ValidationUtil.validateOrderFilterForm(form);
        validateStatusFilter(form.getStatus());
    }

    private TimeRange computeTimeRange(OrderTimeframe timeframe) {
        ZonedDateTime to = ZonedDateTime.now();
        ZonedDateTime from = computeFromTime(timeframe, to);
        return new TimeRange(from, to);
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

    private record TimeRange(ZonedDateTime from, ZonedDateTime to) {}

    private void validateStatusFilter(String status) throws ApiException {
        if (!StringUtils.hasText(status)) return;
        try {
            OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid order status filter: " + status);
        }
    }

    private ZonedDateTime computeFromTime(OrderTimeframe timeframe, ZonedDateTime now) {
        if (timeframe == null) timeframe = OrderTimeframe.LAST_MONTH;

        return switch (timeframe) {
            case LAST_DAY -> now.minusDays(1);
            case LAST_WEEK -> now.minusWeeks(1);
            case LAST_MONTH -> now.minusMonths(1);
        };
    }

    private Page<OrderData> toDataPage(Page<OrderPojo> page) {
        List<OrderData> data = page.getContent()
                .stream()
                .map(OrderHelper::convertToData)
                .collect(Collectors.toList());
        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    private void validateRef(String orderReferenceId) throws ApiException {
        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
    }

    private void normalizeAndValidateCreate(OrderCreateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderCreateForm(form);
        validateCreateOrderForm(form);
    }

    private void validateCreateOrderForm(OrderCreateForm form) throws ApiException {
        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }
        Set<String> seenBarcodes = new HashSet<>();
        for (var item : form.getItems()) {
            if (!StringUtils.hasText(item.getProductBarcode())) {
                throw new ApiException("Product barcode cannot be empty");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ApiException("Invalid quantity");
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
}
