package com.increff.pos.dto;

import com.increff.pos.api.AuthApi;
import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.data.AuthTokenData;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.CreateOperatorForm;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.SignupForm;
import com.increff.pos.util.FormValidationUtil;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthDto {

    @Autowired
    private AuthApi authApi;

    public AuthTokenData signup(SignupForm form) throws ApiException {
        NormalizationUtil.normalizeSignupForm(form);
        FormValidationUtil.validate(form);

        AuthUserPojo user = authApi.signupSupervisor(form.getEmail(), form.getPassword());
        String token = authApi.issueToken(user);
        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthTokenData login(LoginForm form) throws ApiException {
        NormalizationUtil.normalizeLoginForm(form);
        FormValidationUtil.validate(form);

        AuthUserPojo user = authApi.login(form.getEmail(), form.getPassword());
        String token = authApi.issueToken(user);
        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthUserData getCurrentUser(String authHeader) throws ApiException {
        String token = NormalizationUtil.normalizeBearerTokenFromHeader(authHeader);
        ValidationUtil.validateToken(token);
        return authApi.decodeToken(token);
    }

    // ---------------- SUPERVISOR: OPERATORS ----------------

    public AuthUserData createOperator(CreateOperatorForm form) throws ApiException {
        NormalizationUtil.normalizeCreateOperatorForm(form);
        FormValidationUtil.validate(form);

        AuthUserPojo createdOperator =
                authApi.createOperator(form.getEmail(), form.getPassword());
        return new AuthUserData(
                createdOperator.getEmail(),
                createdOperator.getRole()
        );
    }

    public List<AuthUserData> listOperators() {
        return authApi.listOperators()
                .stream()
                .map(this::convertToAuthUserData)
                .toList();
    }

    private AuthUserData convertToAuthUserData(AuthUserPojo user) {
        return new AuthUserData(user.getEmail(), user.getRole());
    }
}
