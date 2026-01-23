package com.increff.pos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@ComponentScan(basePackages = {"com.increff.pos"})
public class SpringConfig {
	public static void main(String[] args) {
		SpringApplication.run(SpringConfig.class, args);
	}
}
