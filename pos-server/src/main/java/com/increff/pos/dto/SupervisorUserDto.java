package com.increff.pos.dto;

import com.increff.pos.api.AuthApi;
import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.CreateOperatorForm;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupervisorUserDto {

    @Autowired
    private AuthApi authApi;

    public AuthUserData create(CreateOperatorForm form) throws ApiException {
        if (form == null) throw new ApiException("Create operator form required");

        form.setEmail(NormalizationUtil.normalizeEmail(form.getEmail()));
        ValidationUtil.validateEmail(form.getEmail());
        ValidationUtil.validatePassword(form.getPassword());

        AuthUserPojo user = authApi.createOperator(form.getEmail(), form.getPassword());
        return new AuthUserData(user.getEmail(), user.getRole());
    }

    public List<AuthUserData> list() {
        return authApi.listOperators().stream()
                .map(u -> new AuthUserData(u.getEmail(), u.getRole()))
                .toList();
    }
}
