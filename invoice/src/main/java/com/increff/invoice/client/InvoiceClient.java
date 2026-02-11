package com.increff.invoice.client;

import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class InvoiceClient {

    private final RestTemplate restTemplate;
    private final String invoiceServiceBaseUrl;

    public InvoiceClient(RestTemplate restTemplate, String invoiceServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.invoiceServiceBaseUrl = invoiceServiceBaseUrl;
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
