package com.increff.pos.config;

import com.increff.pos.helper.JwtHelper;
import com.increff.pos.helper.SupervisorEmailHelper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class AuthStaticInitConfig {

    private final ApplicationProperties properties;

    public AuthStaticInitConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {

        SupervisorEmailHelper.init(
                String.join(",", properties.getAuth().getSupervisors())
        );

        JwtHelper.init(
                properties.getAuth().getJwt().getSecret(),
                properties.getAuth().getJwt().getExpiryMinutes()
        );
    }
}

