package com.increff.pos.controller;

import com.increff.pos.dto.OrderDto;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.PageForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order Management")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderDto orderDto;

    public OrderController(OrderDto orderDto) {
        this.orderDto = orderDto;
    }

    @Operation(summary = "Create a new order")
    @PostMapping("/create")
    public OrderData create(@RequestBody OrderCreateForm form)
            throws ApiException {
        return orderDto.createOrder(form);
    }

    @Operation(summary = "Edit an existing order (full replace)")
    @PutMapping("/edit/{orderReferenceId}")
    public OrderData edit(
            @PathVariable String orderReferenceId,
            @RequestBody OrderCreateForm form
    ) throws ApiException {
        return orderDto.updateOrder(orderReferenceId, form);
    }

    @Operation(summary = "Cancel an order")
    @PutMapping("/cancel/{orderReferenceId}")
    public OrderData cancel(@PathVariable String orderReferenceId)
            throws ApiException {
        return orderDto.cancelOrder(orderReferenceId);
    }

    @Operation(summary = "Get order by reference id")
    @GetMapping("/get/{orderReferenceId}")
    public OrderData get(@PathVariable String orderReferenceId)
            throws ApiException {
        return orderDto.getByOrderReferenceId(orderReferenceId);
    }

    @Operation(summary = "Get all orders (paginated)")
    @PostMapping("/get-all-paginated")
    public Page<OrderData> getAll(@RequestBody PageForm form)
            throws ApiException {
        return orderDto.getAllOrders(form);
    }
}
