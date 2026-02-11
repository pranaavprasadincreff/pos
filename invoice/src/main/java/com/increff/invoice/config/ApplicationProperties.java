package com.increff.invoice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ApplicationProperties {

    @Value("${invoice.self.url}")
    private String invoiceSelfUrl;
}
