package com.increff.pos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.increff.pos")
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableScheduling
public class SpringConfig {
	public static void main(String[] args) {
		SpringApplication.run(SpringConfig.class, args);
	}
}
