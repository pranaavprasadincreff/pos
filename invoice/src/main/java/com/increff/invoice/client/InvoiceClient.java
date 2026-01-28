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

    @Value("${invoice.self.url}")
    private String selfUrl;

    public InvoiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InvoiceData generateInvoice(InvoiceGenerateForm form)
            throws ApiException {
        try {
            return restTemplate.postForObject(
                    selfUrl + "/api/invoices/generate",
                    form,
                    InvoiceData.class
            );
        } catch (HttpStatusCodeException e) {
            throw new ApiException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ApiException("Invoice generation failed");
        }
    }

    public InvoiceData getInvoice(String orderReferenceId)
            throws ApiException {
        try {
            return restTemplate.getForObject(
                    selfUrl + "/api/invoices/get/{id}",
                    InvoiceData.class,
                    orderReferenceId
            );
        } catch (HttpStatusCodeException e) {
            throw new ApiException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ApiException("Invoice fetch failed");
        }
    }
}
