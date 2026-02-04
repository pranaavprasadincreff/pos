package com.increff.pos.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceGenerateForm {

    @NotBlank(message = "Order reference id is required")
    private String orderReferenceId;

    @NotEmpty(message = "At least one invoice item is required")
    @Valid
    private List<InvoiceItemForm> items;
}
