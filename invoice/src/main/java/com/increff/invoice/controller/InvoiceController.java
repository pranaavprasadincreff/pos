package com.increff.invoice.controller;

import com.increff.invoice.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:3000")
@Validated
public class InvoiceController {
    @Autowired
    private InvoiceDto invoiceDto;

    public InvoiceController(InvoiceDto invoiceDto) {
        this.invoiceDto = invoiceDto;
    }

    @PostMapping("/generate")
    public InvoiceData generate(@Valid @RequestBody InvoiceGenerateForm form)
            throws ApiException {
        return invoiceDto.generateInvoice(form);
    }

    @GetMapping("/get/{orderReferenceId}")
    public InvoiceData get(
            @PathVariable
            @NotBlank(message = "Order reference id cannot be empty")
            String orderReferenceId
    ) throws ApiException {
        return invoiceDto.getByOrderReferenceId(orderReferenceId);
    }
}
