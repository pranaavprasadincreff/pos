package com.increff.pos.config;

import com.increff.invoice.client.InvoiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class InvoiceClientConfig {

    @Bean
    public InvoiceClient invoiceClient(RestTemplate restTemplate) {
        return new InvoiceClient(restTemplate);
    }
}
