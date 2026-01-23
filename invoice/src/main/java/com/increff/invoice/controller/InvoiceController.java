package com.increff.invoice.controller;

import com.increff.invoice.dto.InvoiceDto;
import com.increff.invoice.exception.ApiException;
import com.increff.invoice.modal.data.InvoiceData;
import com.increff.invoice.modal.form.InvoiceGenerateForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invoice Management", description = "APIs for generating and fetching invoices")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {
    private final InvoiceDto invoiceDto;

    public InvoiceController(InvoiceDto invoiceDto) {
        this.invoiceDto = invoiceDto;
    }

    @Operation(summary = "Generate invoice for an order (one-time operation)")
    @PostMapping("/generate")
    public InvoiceData generate(@RequestBody InvoiceGenerateForm form) throws ApiException {
        return invoiceDto.generateInvoice(form);
    }

    @Operation(summary = "Get invoice by order reference id")
    @GetMapping("/get/{orderReferenceId}")
    public InvoiceData get(@PathVariable String orderReferenceId) throws ApiException {
        return invoiceDto.getByOrderReferenceId(orderReferenceId);
    }
}
