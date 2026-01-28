package com.increff.pos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

@Configuration
@ComponentScan("com.increff.pos")
@TestPropertySource(properties = {
        "spring.data.mongodb.host=localhost",
        "spring.data.mongodb.port=27017",
        "spring.data.mongodb.database=testdb",
        "spring.mongodb.embedded.version=6.0.1",
        "invoice.service.url=http://localhost:8081"
})
public class TestConfig {
}
