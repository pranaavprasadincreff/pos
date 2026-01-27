package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.helper.OrderHelper;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderCreateItemForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.util.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderDto {
    private final OrderApi orderApi;

    public OrderDto(OrderApi orderApi) {
        this.orderApi = orderApi;
    }

    public OrderData createOrder(OrderCreateForm form) throws ApiException {
        validateCreateOrderForm(form);
        OrderPojo order = OrderHelper.convertCreateFormToEntity(form);
        OrderPojo saved = orderApi.createOrder(order);
        return OrderHelper.convertToData(saved);
    }

    public OrderData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        return OrderHelper.convertToData(order);
    }

    public Page<OrderData> getAllOrders(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<OrderPojo> orderPage = orderApi.getAllOrders(form.getPage(), form.getSize());
        List<OrderData> data = orderPage.getContent()
                .stream()
                .map(OrderHelper::convertToData)
                .collect(Collectors.toList());
        return new PageImpl<>(data, orderPage.getPageable(), orderPage.getTotalElements());
    }


    private void validateCreateOrderForm(OrderCreateForm form) throws ApiException {
        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }
        Set<String> seenBarcodes = new HashSet<>();
        for (OrderCreateItemForm item : form.getItems()) {
            validateItem(item);
            String barcode = item.getProductBarcode().trim().toUpperCase();
            if (!seenBarcodes.add(barcode)) {
                throw new ApiException(
                        "Duplicate product barcode in order: " + barcode
                );
            }
        }
    }

    private void validateItem(OrderCreateItemForm item) throws ApiException {
        if (!StringUtils.hasText(item.getProductBarcode())) {
            throw new ApiException("Product barcode cannot be empty");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ApiException("Invalid ordered quantity");
        }
        if (item.getSellingPrice() == null
                || item.getSellingPrice() <= 0) {
            throw new ApiException("Invalid selling price");
        }
    }
}
