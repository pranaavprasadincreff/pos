package com.increff.pos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Setter
@Getter
@Configuration
@ConfigurationProperties
public class ApplicationProperties {

    private Auth auth = new Auth();
    private Invoice invoice = new Invoice();

    @Setter
    @Getter
    public static class Auth {

        private List<String> supervisors;
        private Jwt jwt = new Jwt();

        @Getter
        @Setter
        public static class Jwt {
            private String secret;
            private long expiryMinutes;
        }
    }

    @Setter
    @Getter
    public static class Invoice {
        private Self self = new Self();

        @Setter
        @Getter
        public static class Self {
            private String url;
        }
    }
}
