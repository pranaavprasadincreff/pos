package com.increff.pos.api;

import com.increff.pos.dao.AuthUserDao;
import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.helper.JwtHelper;
import com.increff.pos.helper.PasswordHelper;
import com.increff.pos.helper.SupervisorEmailHelper;
import com.increff.pos.model.constants.Role;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthApiImpl implements AuthApi {
    @Autowired
    private AuthUserDao authUserDao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public AuthUserPojo signupSupervisor(String email, String password) throws ApiException {
        ensureEmailIsSupervisor(email);
        ensureEmailIsUnique(email);
        AuthUserPojo supervisorUser = createAuthUser(email, password, Role.SUPERVISOR);
        return authUserDao.save(supervisorUser);
    }

    @Override
    public AuthUserPojo login(String email, String password) throws ApiException {
        AuthUserPojo authUser = authUserDao.findByEmail(email);
        ensureCredentialsAreValid(authUser, password);
        return authUser;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public AuthUserPojo createOperator(String email, String password) throws ApiException {
        ensureEmailIsNotSupervisor(email);
        ensureEmailIsUnique(email);
        AuthUserPojo operatorUser = createAuthUser(email, password, Role.OPERATOR);
        return authUserDao.save(operatorUser);
    }

    @Override
    public List<AuthUserPojo> listOperators() {
        return authUserDao.findByRole(Role.OPERATOR);
    }

    @Override
    public String issueToken(AuthUserPojo authUser) {
        return JwtHelper.createToken(authUser.getId(), authUser.getEmail(), authUser.getRole());
    }

    @Override
    public AuthUserData decodeToken(String token) throws ApiException {
        Claims claims = parseJwtClaims(token);
        String email = claims.get("email", String.class);
        Role role = Role.valueOf(claims.get("role", String.class));
        return new AuthUserData(email, role);
    }

    private void ensureEmailIsSupervisor(String email) throws ApiException {
        if (!SupervisorEmailHelper.isSupervisorEmail(email)) {
            throw new ApiException("Operators are invite-only. Ask a supervisor to create your login.");
        }
    }

    private void ensureEmailIsNotSupervisor(String email) throws ApiException {
        if (SupervisorEmailHelper.isSupervisorEmail(email)) {
            throw new ApiException("This email is configured as a supervisor in application.properties. " +
                    "Remove it from auth.supervisors to create as operator.");
        }
    }

    private void ensureEmailIsUnique(String email) throws ApiException {
        AuthUserPojo existingUser = authUserDao.findByEmail(email);
        if (existingUser != null) {
            throw new ApiException("User already exists");
        }
    }

    private void ensureCredentialsAreValid(AuthUserPojo authUser, String rawPassword) throws ApiException {
        boolean isValidUser = authUser != null && PasswordHelper.matches(rawPassword, authUser.getPasswordHash());
        if (!isValidUser) {
            throw new ApiException("Invalid email or password");
        }
    }

    private AuthUserPojo createAuthUser(String email, String rawPassword, Role role) {
        AuthUserPojo authUser = new AuthUserPojo();
        authUser.setEmail(email);
        authUser.setPasswordHash(PasswordHelper.encode(rawPassword));
        authUser.setRole(role);
        return authUser;
    }

    private Claims parseJwtClaims(String token) throws ApiException {
        try {
            return JwtHelper.parseClaims(token);
        } catch (Exception e) {
            throw new ApiException("Invalid token");
        }
    }
}
