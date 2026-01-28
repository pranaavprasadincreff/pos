package com.increff.invoice.controller;

import com.increff.invoice.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {

    private final InvoiceDto invoiceDto;

    public InvoiceController(InvoiceDto invoiceDto) {
        this.invoiceDto = invoiceDto;
    }

    @PostMapping("/generate")
    public InvoiceData generate(@RequestBody InvoiceGenerateForm form)
            throws ApiException {
        return invoiceDto.generateInvoice(form);
    }

    @GetMapping("/get/{orderReferenceId}")
    public InvoiceData get(@PathVariable String orderReferenceId)
            throws ApiException {
        return invoiceDto.getByOrderReferenceId(orderReferenceId);
    }
}
