package com.increff.pos.controller;

import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invoice Management", description = "APIs for generating and fetching invoices")
@Validated
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/pos/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceDto invoiceDto;

    @Operation(summary = "Generate invoice for an order (one-time operation)")
    @PostMapping("/generate/{orderReferenceId}")
    public InvoiceData generate(
            @PathVariable
            @NotBlank(message = "Order reference id cannot be empty")
            @Size(max = 50, message = "Order reference id too long")
            String orderReferenceId
    ) throws ApiException {
        return invoiceDto.generateInvoice(orderReferenceId);
    }

    @Operation(summary = "Get invoice by order reference id")
    @GetMapping("/get/{orderReferenceId}")
    public InvoiceData get(
            @PathVariable
            @NotBlank(message = "Order reference id cannot be empty")
            @Size(max = 50, message = "Order reference id too long")
            String orderReferenceId
    ) throws ApiException {
        return invoiceDto.getInvoice(orderReferenceId);
    }
}
