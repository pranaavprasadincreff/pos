package com.increff.pos.model.form;

import lombok.Data;

import java.util.List;

@Data
public class InvoiceGenerateForm {
    private String orderReferenceId;
    private List<InvoiceItemForm> items;
}
