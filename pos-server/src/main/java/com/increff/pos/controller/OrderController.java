package com.increff.pos.controller;

import com.increff.pos.dto.OrderDto;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.form.OrderCreateForm;
import com.increff.pos.model.form.PageForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order Management",
        description = "APIs for creating and viewing orders")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderDto orderDto;

    public OrderController(OrderDto orderDto) {
        this.orderDto = orderDto;
    }

    @Operation(summary = "Create a new order")
    @RequestMapping(path = "/create", method = RequestMethod.POST)
    public OrderData create(@RequestBody OrderCreateForm form) throws ApiException {
        return orderDto.createOrder(form);
    }

    @Operation(summary = "Get order by reference id")
    @RequestMapping(path = "/get/{orderReferenceId}", method = RequestMethod.GET)
    public OrderData get(@PathVariable String orderReferenceId) throws ApiException {
        return orderDto.getByOrderReferenceId(orderReferenceId);
    }

    @Operation(summary = "Get all orders (paginated)")
    @RequestMapping(path = "/get-all-paginated", method = RequestMethod.POST)
    public Page<OrderData> getAll(@RequestBody PageForm form) throws ApiException {
        return orderDto.getAllOrders(form);
    }

}
