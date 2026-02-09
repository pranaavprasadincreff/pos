package com.increff.pos.dto;

import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
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
import com.increff.pos.util.FormValidator;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

@Component
public class OrderDto {

    @Autowired
    private OrderFlow orderFlow;

    // TODO move to flow, flow can call the product api
    @Autowired
    private ProductApi productApi;

    @Autowired
    private FormValidator formValidator;

    public OrderData createOrder(OrderCreateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderCreateForm(form);
        formValidator.validate(form);

        List<String> barcodes = extractBarcodes(form.getItems());
        List<Integer> quantities = extractQuantities(form.getItems());
        List<Double> sellingPrices = extractSellingPrices(form.getItems());

        OrderPojo orderToCreate = OrderHelper.buildOrderForCreate(quantities, sellingPrices);
        OrderPojo createdOrder = orderFlow.create(orderToCreate, barcodes);

        return buildOrderData(createdOrder);
    }

    public OrderData updateOrder(OrderUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeOrderUpdateForm(form);
        formValidator.validate(form);

        String orderReferenceId = form.getOrderReferenceId();
        ValidationUtil.validateOrderReferenceId(orderReferenceId);

        List<String> barcodes = extractBarcodes(form.getItems());
        List<Integer> quantities = extractQuantities(form.getItems());
        List<Double> sellingPrices = extractSellingPrices(form.getItems());

        OrderPojo updateRequest = OrderHelper.buildOrderForCreate(quantities, sellingPrices);
        OrderPojo updatedOrder = orderFlow.update(orderReferenceId, updateRequest, barcodes);

        return buildOrderData(updatedOrder);
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
        formValidator.validate(form);

        if (form.getOrderReferenceId() != null) {
            ValidationUtil.validateOrderReferenceId(form.getOrderReferenceId());
        }

        TimeRange timeRange = computeTimeRange(form.getTimeframe());

        Page<OrderPojo> orders = orderFlow.search(
                form.getOrderReferenceId(),
                form.getStatus(),
                timeRange.from(),
                timeRange.to(),
                form.getPage(),
                form.getSize()
        );

        return buildOrderDataPage(orders);
    }

    private Page<OrderData> buildOrderDataPage(Page<OrderPojo> orders) {
        List<OrderData> data = orders.getContent().stream()
                .map(this::buildOrderData)
                .toList();

        return new PageImpl<>(data, orders.getPageable(), orders.getTotalElements());
    }

    private OrderData buildOrderData(OrderPojo order) {
        List<String> productIds = order.getOrderItems().stream()
                .map(OrderItemPojo::getProductId)
                .distinct()
                .toList();

        Map<String, ProductPojo> productById = fetchProductsById(productIds);

        List<OrderItemData> items = order.getOrderItems().stream()
                .map(item -> buildOrderItemData(item, productById.get(item.getProductId())))
                .toList();

        return OrderHelper.buildOrderData(order, items);
    }

    private Map<String, ProductPojo> fetchProductsById(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();

        List<ProductPojo> products = productApi.findByIds(productIds);

        Map<String, ProductPojo> productById = new HashMap<>();
        for (ProductPojo p : products) {
            productById.put(p.getId(), p);
        }
        return productById;
    }

    private OrderItemData buildOrderItemData(OrderItemPojo item, ProductPojo product) {
        OrderItemData data = new OrderItemData();
        data.setQuantity(item.getOrderedQuantity());
        data.setSellingPrice(item.getSellingPrice());
        data.setProductBarcode(product == null ? null : product.getBarcode());
        return data;
    }

    private List<String> extractBarcodes(List<OrderCreateItemForm> items) throws ApiException {
        List<String> barcodes = items.stream()
                .map(OrderCreateItemForm::getProductBarcode)
                .toList();

        Set<String> unique = new HashSet<>();
        for (String barcode : barcodes) {
            if (!unique.add(barcode)) {
                throw new ApiException("Duplicate product barcode in order: " + barcode);
            }
        }
        return barcodes;
    }

    private List<Integer> extractQuantities(List<OrderCreateItemForm> items) {
        return items.stream().map(OrderCreateItemForm::getQuantity).toList();
    }

    private List<Double> extractSellingPrices(List<OrderCreateItemForm> items) {
        return items.stream().map(OrderCreateItemForm::getSellingPrice).toList();
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
