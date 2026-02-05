package com.increff.invoice.client;

import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class InvoiceClient {
    private final RestTemplate restTemplate;

    @Value("${invoice.self.url}") // keep your current property name
    private String invoiceServiceBaseUrl;

    public InvoiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InvoiceData generateInvoice(InvoiceGenerateForm invoiceRequest) throws ApiException {
        String url = invoiceServiceBaseUrl + "/api/invoices/generate";

        try {
            return restTemplate.postForObject(url, invoiceRequest, InvoiceData.class);
        } catch (HttpStatusCodeException e) {
            throw new ApiException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ApiException("Invoice generation failed: " + e.getMessage());
        }
    }

    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        String url = invoiceServiceBaseUrl + "/api/invoices/get/{orderReferenceId}";

        try {
            return restTemplate.getForObject(url, InvoiceData.class, orderReferenceId);
        } catch (HttpStatusCodeException e) {
            throw new ApiException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ApiException("Invoice fetch failed: " + e.getMessage());
        }
    }
}
