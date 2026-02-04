package com.increff.pos.dto;

import com.increff.pos.api.AuthApi;
import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.data.AuthTokenData;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.SignupForm;
import com.increff.pos.util.NormalizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthDto {
    @Autowired
    private AuthApi authApi;

    public AuthTokenData signup(SignupForm form) throws ApiException {
        SignupForm normalizedForm = normalizeSignupForm(form);
        AuthUserPojo user = authApi.signupSupervisor(
                normalizedForm.getEmail(),
                normalizedForm.getPassword()
        );

        String token = authApi.issueToken(user);
        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthTokenData login(LoginForm form) throws ApiException {
        LoginForm normalizedForm = normalizeLoginForm(form);
        AuthUserPojo user = authApi.login(
                normalizedForm.getEmail(),
                normalizedForm.getPassword()
        );

        String token = authApi.issueToken(user);
        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthUserData me(String token) throws ApiException {
        validateToken(token);
        return authApi.decodeToken(token);
    }

    private SignupForm normalizeSignupForm(SignupForm form) throws ApiException {
        if (form == null) throw new ApiException("Signup form required");
        form.setEmail(NormalizationUtil.normalizeEmail(form.getEmail()));
        return form;
    }

    private LoginForm normalizeLoginForm(LoginForm form) throws ApiException {
        if (form == null) throw new ApiException("Login form required");
        form.setEmail(NormalizationUtil.normalizeEmail(form.getEmail()));
        return form;
    }

    private void validateToken(String token) throws ApiException {
        if (token == null || token.isBlank()) {
            throw new ApiException("Invalid token");
        }
    }
}
