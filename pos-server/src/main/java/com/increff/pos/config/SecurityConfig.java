package com.increff.pos.config;

import com.increff.pos.auth.ApiAccessRules;
import com.increff.pos.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
            throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/api/auth/**").permitAll();
            auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();

            auth.requestMatchers("/api/supervisor/**").hasRole("SUPERVISOR");

            // Operator allowlist (also lets supervisors pass)
            for (ApiAccessRules.Rule rule : ApiAccessRules.OPERATOR_ALLOWED) {
                auth.requestMatchers(rule.method(), rule.pattern())
                        .hasAnyRole("OPERATOR", "SUPERVISOR");
            }

            // Everything else: supervisor only
            auth.anyRequest().hasRole("SUPERVISOR");
        });

        return http.build();
    }
}
