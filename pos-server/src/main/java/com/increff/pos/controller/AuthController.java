package com.increff.pos.controller;

import com.increff.pos.dto.AuthDto;
import com.increff.pos.model.data.AuthTokenData;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.SignupForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthDto authDto;

    @PostMapping("/signup")
    public AuthTokenData signup(@Valid @RequestBody SignupForm form) throws ApiException {
        return authDto.signup(form);
    }

    @PostMapping("/login")
    public AuthTokenData login(@Valid @RequestBody LoginForm form) throws ApiException {
        return authDto.login(form);
    }

    @GetMapping("/me")
    public AuthUserData me(
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            @NotBlank(message = "Missing Authorization header")
            String authHeader
    ) throws ApiException {
        return authDto.me(extractBearerToken(authHeader));
    }

    private String extractBearerToken(String authHeader) throws ApiException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException("Missing Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new ApiException("Missing Authorization header");
        }

        return token;
    }
}
