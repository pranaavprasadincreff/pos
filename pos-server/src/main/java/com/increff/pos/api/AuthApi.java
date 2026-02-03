package com.increff.pos.api;

import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.constants.Role;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;

import java.util.List;

public interface AuthApi {
    AuthUserPojo signupSupervisor(String email, String password) throws ApiException;
    AuthUserPojo login(String email, String password) throws ApiException;

    AuthUserPojo createOperator(String email, String password) throws ApiException;
    List<AuthUserPojo> listOperators();

    String issueToken(AuthUserPojo user);

    AuthUserData decodeToken(String token) throws ApiException;
}
