package com.increff.invoice.api;

import com.increff.invoice.dao.InvoiceDao;
import com.increff.invoice.db.InvoicePojo;
import com.increff.invoice.exception.ApiException;
import com.increff.invoice.helper.FopPdfHelper;
import com.increff.invoice.helper.InvoiceTemplateHelper;
import com.increff.invoice.modal.form.InvoiceGenerateForm;
import com.mongodb.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class InvoiceApiImpl implements InvoiceApi {
    private final InvoiceDao invoiceDao;

    public InvoiceApiImpl(InvoiceDao invoiceDao) {
        this.invoiceDao = invoiceDao;
    }

    @Override
    public InvoicePojo generateInvoice(InvoiceGenerateForm form) throws ApiException {
        ensureInvoiceDoesNotExist(form.getOrderReferenceId());
        InvoicePojo invoice = createInvoiceEntity(form);
        try {
            return invoiceDao.save(invoice);
        } catch (DuplicateKeyException e) {
            // Safety net for race conditions
            throw new ApiException("Invoice already exists for order: " + form.getOrderReferenceId());
        }
    }

    @Override
    public InvoicePojo getByOrderReferenceId(String orderReferenceId) throws ApiException {
        InvoicePojo invoice = invoiceDao.findByOrderReferenceId(orderReferenceId);
        if (invoice == null) {
            throw new ApiException("Invoice not found for order: " + orderReferenceId);
        }
        return invoice;
    }

    private void ensureInvoiceDoesNotExist(String orderReferenceId) throws ApiException {
        if (invoiceDao.findByOrderReferenceId(orderReferenceId) != null) {
            throw new ApiException(
                    "Invoice already generated. Use GET /invoice/{orderReferenceId}"
            );
        }
    }

    private InvoicePojo createInvoiceEntity(InvoiceGenerateForm form) throws ApiException {
        String xslFo = InvoiceTemplateHelper.createInvoiceXslFo(form);
        String pdfBase64 = FopPdfHelper.generatePdfBase64(
                form.getOrderReferenceId(),
                xslFo
        );

        InvoicePojo invoice = new InvoicePojo();
        invoice.setOrderReferenceId(form.getOrderReferenceId());
        invoice.setPdfBase64(pdfBase64);
        invoice.setPdfPath("invoices/" + form.getOrderReferenceId() + ".pdf");
        return invoice;
    }
}
