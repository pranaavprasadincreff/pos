package com.increff.pos.controller;

import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invoice Management", description = "APIs for generating and fetching invoices")
@RestController
@RequestMapping("/api/pos/invoice")
public class InvoiceController {

    private final InvoiceDto invoiceDto;

    public InvoiceController(InvoiceDto invoiceDto) {
        this.invoiceDto = invoiceDto;
    }

    @RequestMapping(path = "/generate/{orderReferenceId}", method = RequestMethod.POST)
    public InvoiceData generate(@PathVariable String orderReferenceId)
            throws ApiException {

        return invoiceDto.generateInvoice(orderReferenceId);
    }

    @RequestMapping(path = "/get/{orderReferenceId}", method = RequestMethod.GET)
    public InvoiceData get(@PathVariable String orderReferenceId)
            throws ApiException {

        return invoiceDto.getInvoice(orderReferenceId);
    }
}

