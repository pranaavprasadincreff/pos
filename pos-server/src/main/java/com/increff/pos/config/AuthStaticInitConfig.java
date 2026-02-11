package com.increff.pos.config;

import com.increff.pos.helper.JwtHelper;
import com.increff.pos.helper.SupervisorEmailHelper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthStaticInitConfig {

    private final ApplicationProperties properties;

    public AuthStaticInitConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        SupervisorEmailHelper.init(properties.getSupervisorsCsv());

        JwtHelper.init(
                properties.getJwtSecret(),
                properties.getJwtExpiryMinutes()
        );
    }
}
