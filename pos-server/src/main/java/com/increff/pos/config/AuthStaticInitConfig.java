package com.increff.pos.config;

import com.increff.pos.helper.JwtHelper;
import com.increff.pos.helper.SupervisorEmailHelper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class AuthStaticInitConfig {

    private final Environment env;

    public AuthStaticInitConfig(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        String supervisors = env.getProperty("auth.supervisors", "");
        SupervisorEmailHelper.init(supervisors);

        String secret = env.getProperty("auth.jwt.secret");
        String expiryStr = env.getProperty("auth.jwt.expiryMinutes", "120");

        long expiry;
        try {
            expiry = Long.parseLong(expiryStr);
        } catch (Exception e) {
            expiry = 120;
        }

        JwtHelper.init(secret, expiry);
    }
}
