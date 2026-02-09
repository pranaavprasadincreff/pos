package com.increff.pos.controller;

import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invoice Management", description = "APIs for generating and fetching invoices")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceDto invoiceDto;

    @Operation(summary = "Generate invoice for an order (one-time operation)")
    @PostMapping("/{orderReferenceId}")
    public InvoiceData generate(@PathVariable String orderReferenceId) throws ApiException {
        return invoiceDto.generateInvoice(orderReferenceId);
    }

    @Operation(summary = "Get invoice by order reference id")
    @GetMapping("/{orderReferenceId}")
    public InvoiceData get(@PathVariable String orderReferenceId) throws ApiException {
        return invoiceDto.getInvoice(orderReferenceId);
    }
}
