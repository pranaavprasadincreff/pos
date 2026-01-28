package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.helper.OrderHelper;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateForm;
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

    // ---------- Create ----------

    public OrderData createOrder(OrderCreateForm form) throws ApiException {
        validateCreateOrderForm(form);
        OrderPojo order = OrderHelper.convertCreateFormToEntity(form);
        return OrderHelper.convertToData(orderApi.createOrder(order));
    }

    // ---------- Edit ----------

    public OrderData updateOrder(String orderReferenceId, OrderCreateForm form)
            throws ApiException {

        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        validateCreateOrderForm(form);

        OrderPojo updated = OrderHelper.convertCreateFormToEntity(form);
        OrderPojo saved =
                orderApi.updateOrder(orderReferenceId, updated);

        return OrderHelper.convertToData(saved);
    }

    // ---------- Cancel ----------

    public void cancelOrder(String orderReferenceId) throws ApiException {
        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        orderApi.cancelOrder(orderReferenceId);
    }

    // ---------- Read ----------

    public OrderData getByOrderReferenceId(String orderReferenceId)
            throws ApiException {

        if (!StringUtils.hasText(orderReferenceId)) {
            throw new ApiException("Order reference id cannot be empty");
        }
        return OrderHelper.convertToData(
                orderApi.getByOrderReferenceId(orderReferenceId)
        );
    }

    public Page<OrderData> getAllOrders(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<OrderPojo> page =
                orderApi.getAllOrders(form.getPage(), form.getSize());

        List<OrderData> data =
                page.getContent()
                        .stream()
                        .map(OrderHelper::convertToData)
                        .collect(Collectors.toList());

        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    // ---------- Validation ----------

    private void validateCreateOrderForm(OrderCreateForm form)
            throws ApiException {

        if (form == null || CollectionUtils.isEmpty(form.getItems())) {
            throw new ApiException("Order must contain at least one item");
        }

        Set<String> seenBarcodes = new HashSet<>();

        form.getItems().forEach(item -> {
            if (!StringUtils.hasText(item.getProductBarcode())) {
                throw new RuntimeException("Product barcode cannot be empty");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Invalid quantity");
            }
            if (item.getSellingPrice() == null || item.getSellingPrice() <= 0) {
                throw new RuntimeException("Invalid selling price");
            }

            String barcode = item.getProductBarcode().trim().toUpperCase();
            if (!seenBarcodes.add(barcode)) {
                throw new RuntimeException(
                        "Duplicate product barcode in order: " + barcode
                );
            }
        });
    }
}
