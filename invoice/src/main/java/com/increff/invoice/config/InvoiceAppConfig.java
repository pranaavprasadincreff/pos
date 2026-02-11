package com.increff.invoice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.increff")
public class InvoiceAppConfig {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceAppConfig.class, args);
    }
}
