package com.increff.invoice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "invoice")
@Getter
@Setter
public class ApplicationProperties {
    private String baseUrl;
}
