package com.increff.pos.dto;

import com.increff.pos.api.AuthApi;
import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.data.AuthTokenData;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.SignupForm;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthDto {

    @Autowired
    private AuthApi authApi;

    public AuthTokenData signup(SignupForm form) throws ApiException {
        if (form == null) throw new ApiException("Signup form required");

        form.setEmail(NormalizationUtil.normalizeEmail(form.getEmail()));
        ValidationUtil.validateEmail(form.getEmail());
        ValidationUtil.validatePassword(form.getPassword());

        AuthUserPojo user = authApi.signupSupervisor(form.getEmail(), form.getPassword());
        String token = authApi.issueToken(user);

        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthTokenData login(LoginForm form) throws ApiException {
        if (form == null) throw new ApiException("Login form required");

        form.setEmail(NormalizationUtil.normalizeEmail(form.getEmail()));
        ValidationUtil.validateEmail(form.getEmail());
        ValidationUtil.validatePassword(form.getPassword());

        AuthUserPojo user = authApi.login(form.getEmail(), form.getPassword());
        String token = authApi.issueToken(user);

        return new AuthTokenData(user.getEmail(), user.getRole(), token);
    }

    public AuthUserData me(String token) throws ApiException {
        return authApi.decodeToken(token);
    }


}
