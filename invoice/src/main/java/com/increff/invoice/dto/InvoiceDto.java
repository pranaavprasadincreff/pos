package com.increff.invoice.dto;

import com.increff.invoice.helper.FopPdfHelper;
import com.increff.invoice.helper.InvoiceTemplateHelper;
import com.increff.invoice.util.ValidationUtil;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {
    public InvoiceData generateInvoice(InvoiceGenerateForm form) throws ApiException {
        ValidationUtil.validateInvoiceGenerateForm(form);

        String xslFo = InvoiceTemplateHelper.createInvoiceXslFo(form);
        String pdfBase64 = FopPdfHelper.generatePdfBase64(form.getOrderReferenceId(), xslFo);

        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId(form.getOrderReferenceId());
        data.setPdfBase64(pdfBase64);
        data.setPdfPath("invoices/" + form.getOrderReferenceId() + ".pdf");

        return data;
    }

    public InvoiceData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        ValidationUtil.validateOrderReferenceId(orderReferenceId);

        String pdfPath = "invoices/" + orderReferenceId + ".pdf";
        try {
            byte[] pdfBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pdfPath));
            String pdfBase64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);

            InvoiceData data = new InvoiceData();
            data.setOrderReferenceId(orderReferenceId);
            data.setPdfBase64(pdfBase64);
            data.setPdfPath(pdfPath);
            return data;
        } catch (java.io.IOException e) {
            throw new ApiException("Invoice not found for order: " + orderReferenceId);
        }
    }
}
