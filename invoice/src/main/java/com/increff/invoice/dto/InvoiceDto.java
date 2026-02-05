package com.increff.invoice.dto;

import com.increff.invoice.helper.FopPdfHelper;
import com.increff.invoice.helper.InvoiceTemplateHelper;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class InvoiceDto {

    public InvoiceData generateInvoice(InvoiceGenerateForm invoiceRequest) throws ApiException {
        String orderReferenceId = invoiceRequest.getOrderReferenceId();

        String invoiceXslFo = InvoiceTemplateHelper.createInvoiceXslFo(invoiceRequest);
        String pdfBase64 = FopPdfHelper.generatePdfAndReturnBase64(orderReferenceId, invoiceXslFo);
        String pdfPath = FopPdfHelper.buildPdfPath(orderReferenceId);

        return toInvoiceData(orderReferenceId, pdfPath, pdfBase64);
    }

    public InvoiceData getByOrderReferenceId(String orderReferenceId) throws ApiException {
        String pdfPath = FopPdfHelper.buildPdfPath(orderReferenceId);

        try {
            byte[] pdfBytes = Files.readAllBytes(Path.of(pdfPath));
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            return toInvoiceData(orderReferenceId, pdfPath, pdfBase64);
        } catch (Exception e) {
            throw new ApiException("Invoice not found for order: " + orderReferenceId);
        }
    }

    private static InvoiceData toInvoiceData(String orderReferenceId, String pdfPath, String pdfBase64) {
        InvoiceData response = new InvoiceData();
        response.setOrderReferenceId(orderReferenceId);
        response.setPdfPath(pdfPath);
        response.setPdfBase64(pdfBase64);
        return response;
    }
}
