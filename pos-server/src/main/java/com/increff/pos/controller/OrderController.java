package com.increff.pos.controller;

import com.increff.pos.dto.OrderDto;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.OrderFilterForm;
import com.increff.pos.model.form.PageForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order Management")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

    @Autowired
    private OrderDto orderDto;

    @Operation(summary = "Create a new order")
    @PostMapping("/create")
    public OrderData create(@Valid @RequestBody OrderCreateForm form) throws ApiException {
        return orderDto.createOrder(form);
    }

    @Operation(summary = "Edit an existing order (full replace)")
    @PutMapping("/edit/{orderReferenceId}")
    public OrderData edit(
            @PathVariable String orderReferenceId,
            @Valid @RequestBody OrderCreateForm form
    ) throws ApiException {
        return orderDto.updateOrder(orderReferenceId, form);
    }

    @Operation(summary = "Cancel an order")
    @PutMapping("/cancel/{orderReferenceId}")
    public OrderData cancel(@PathVariable String orderReferenceId) throws ApiException {
        return orderDto.cancelOrder(orderReferenceId);
    }

    @Operation(summary = "Get order by reference id")
    @GetMapping("/get/{orderReferenceId}")
    public OrderData get(@PathVariable String orderReferenceId) throws ApiException {
        return orderDto.getByOrderReferenceId(orderReferenceId);
    }

    @Operation(summary = "Get all orders (paginated)")
    @PostMapping("/get-all-paginated")
    public Page<OrderData> getAll(@Valid @RequestBody PageForm form) throws ApiException {
        return orderDto.getAllOrders(form);
    }

    @Operation(summary = "Filter orders (paginated)")
    @PostMapping("/filter")
    public Page<OrderData> filter(@Valid @RequestBody OrderFilterForm form) throws ApiException {
        return orderDto.filterOrders(form);
    }
}
