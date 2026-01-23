package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.helper.OrderHelper;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderCreateItemForm;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    private void validateCreateOrderForm(OrderCreateForm form) throws ApiException {
        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }
        for (OrderCreateItemForm item : form.getItems()) {
            validateItem(item);
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
