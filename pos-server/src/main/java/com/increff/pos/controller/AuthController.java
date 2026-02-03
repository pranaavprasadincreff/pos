package com.increff.pos.controller;

import com.increff.pos.dto.AuthDto;
import com.increff.pos.model.data.AuthTokenData;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.SignupForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthDto authDto;

    @PostMapping("/signup")
    public AuthTokenData signup(@RequestBody SignupForm form) throws ApiException {
        return authDto.signup(form);
    }

    @PostMapping("/login")
    public AuthTokenData login(@RequestBody LoginForm form) throws ApiException {
        return authDto.login(form);
    }

    @GetMapping("/me")
    public AuthUserData me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) throws ApiException {
        return authDto.me(extractToken(authHeader));
    }

    private String extractToken(String header) throws ApiException {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException("Missing Authorization header");
        }
        return header.substring(7);
    }
}
