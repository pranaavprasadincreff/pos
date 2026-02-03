package com.increff.pos.wrapper;

import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class InvoiceClientWrapperImpl implements InvoiceClientWrapper {
    private final RestTemplate restTemplate;

    @Value("${invoice.service.url}")
    private String invoiceServiceUrl;

    public InvoiceClientWrapperImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public InvoiceData generateInvoice(InvoiceGenerateForm form) throws ApiException {
        try {
            ResponseEntity<InvoiceData> response =
                    restTemplate.postForEntity(
                            invoiceServiceUrl + "/api/invoices/generate",
                            form,
                            InvoiceData.class
                    );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new ApiException(
                    "Invoice service error: " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new ApiException("Invoice service call failed");
        }
    }

    @Override
    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        try {
            return restTemplate.getForObject(
                    invoiceServiceUrl + "/api/invoices/get/{orderReferenceId}",
                    InvoiceData.class,
                    orderReferenceId
            );
        } catch (HttpStatusCodeException e) {
            throw new ApiException(
                    "Invoice service error: " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new ApiException("Invoice service call failed");
        }
    }
}
