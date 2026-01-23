package com.increff.invoice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableMongoRepositories(basePackages = "com.increff.invoice.dao")
@ComponentScan(basePackages = "com.increff.invoice")
public class InvoiceAppConfig {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceAppConfig.class, args);
    }
}

