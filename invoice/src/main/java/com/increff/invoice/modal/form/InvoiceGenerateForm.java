package com.increff.invoice.modal.form;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceGenerateForm {
    private String orderReferenceId;
    private List<InvoiceItemForm> items;
}
