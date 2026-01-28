package com.increff.pos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication(scanBasePackages = "com.increff.pos")
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class SpringConfig {

	public static void main(String[] args) {
		SpringApplication.run(SpringConfig.class, args);
	}
}
