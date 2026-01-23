package com.increff.pos.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceClient {
    private static final String INVOICE_BASE_URL = "http://localhost:8081/api/invoice";

    private final RestTemplate restTemplate;
    private final OrderApi orderApi;
    private final ProductApi productApi;

    public InvoiceClient(
            RestTemplate restTemplate,
            OrderApi orderApi,
            ProductApi productApi
    ) {
        this.restTemplate = restTemplate;
        this.orderApi = orderApi;
        this.productApi = productApi;
    }

    @Retry(name = "invoiceService")
    @CircuitBreaker(
            name = "invoiceService",
            fallbackMethod = "generateFallback"
    )
    public InvoiceData generateInvoice(String orderReferenceId) throws ApiException {
        InvoiceGenerateForm form = buildInvoiceForm(orderReferenceId);
        try {
            ResponseEntity<InvoiceData> response =
                    restTemplate.postForEntity(
                            INVOICE_BASE_URL + "/generate",
                            form,
                            InvoiceData.class
                    );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ApiException(extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new ApiException("Invoice service unavailable. Please try again later."
            );
        }
    }

    @CircuitBreaker(
            name = "invoiceService",
            fallbackMethod = "getFallback"
    )
    public InvoiceData getInvoice(String orderReferenceId) throws ApiException {
        orderApi.getByOrderReferenceId(orderReferenceId);
        try {
            return restTemplate.getForObject(
                    INVOICE_BASE_URL + "/get/" + orderReferenceId,
                    InvoiceData.class
            );
        } catch (HttpClientErrorException e) {
            throw new ApiException(extractMessage(e));
        }
    }

    private InvoiceGenerateForm buildInvoiceForm(String orderReferenceId) throws ApiException {
        OrderPojo order = orderApi.getByOrderReferenceId(orderReferenceId);
        List<InvoiceItemForm> items =
                order.getOrderItems()
                        .stream()
                        .map(item -> {
                            try {
                                return convertToInvoiceItem(item);
                            } catch (ApiException e) {
                                throw new RuntimeException(e);
                            }
                            })
                        .collect(Collectors.toList());

        InvoiceGenerateForm form = new InvoiceGenerateForm();
        form.setOrderReferenceId(orderReferenceId);
        form.setItems(items);
        return form;
    }

    private InvoiceItemForm convertToInvoiceItem(OrderItemPojo item) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(item.getProductBarcode());
        InvoiceItemForm form = new InvoiceItemForm();
        form.setBarcode(product.getBarcode());
        form.setProductName(product.getName());
        form.setQuantity(item.getOrderedQuantity());
        form.setSellingPrice(item.getSellingPrice());
        return form;
    }

    private String extractMessage(HttpClientErrorException e) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node =
                    mapper.readTree(e.getResponseBodyAsString());
            return node.get("message").asText();
        } catch (Exception ex) {
            return "Invoice service error";
        }
    }

    private InvoiceData generateFallback(String orderReferenceId, Throwable t) throws ApiException {
        throw new ApiException(
                "Invoice service unavailable. Please try again later."
        );
    }

    private InvoiceData getFallback(String orderReferenceId, Throwable t) throws ApiException {
        throw new ApiException(
                "Unable to fetch invoice at the moment."
        );
    }
}


