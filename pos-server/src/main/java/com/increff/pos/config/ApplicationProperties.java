package com.increff.pos.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ApplicationProperties {

    @Value("${auth.supervisors:}")
    private String supervisorsCsv;

    @Value("${auth.jwt.secret:}")
    private String jwtSecret;

    @Value("${auth.jwt.expiryMinutes:120}")
    private long jwtExpiryMinutes;

    @Value("${invoice.self.url:}")
    private String invoiceSelfUrl;
}
