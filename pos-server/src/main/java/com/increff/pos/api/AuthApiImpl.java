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
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthApiImpl implements AuthApi {

    @Autowired
    private AuthUserDao dao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public AuthUserPojo signupSupervisor(String email, String password) throws ApiException {
        if (!SupervisorEmailHelper.isSupervisorEmail(email)) {
            throw new ApiException("Operators are invite-only. Ask a supervisor to create your login.");
        }
        ensureEmailUnique(email);

        AuthUserPojo u = new AuthUserPojo();
        u.setEmail(email);
        u.setPasswordHash(PasswordHelper.encode(password));
        u.setRole(Role.SUPERVISOR);
        return dao.save(u);
    }

    @Override
    public AuthUserPojo login(String email, String password) throws ApiException {
        AuthUserPojo user = dao.findByEmail(email);
        if (user == null || !PasswordHelper.matches(password, user.getPasswordHash())) {
            throw new ApiException("Invalid email or password");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public AuthUserPojo createOperator(String email, String password) throws ApiException {
        if (SupervisorEmailHelper.isSupervisorEmail(email)) {
            throw new ApiException("This email is configured as a supervisor in application.properties. Remove it from auth.supervisors to create as operator.");
        }
        ensureEmailUnique(email);

        AuthUserPojo u = new AuthUserPojo();
        u.setEmail(email);
        u.setPasswordHash(PasswordHelper.encode(password));
        u.setRole(Role.OPERATOR);
        return dao.save(u);
    }

    @Override
    public List<AuthUserPojo> listOperators() {
        return dao.findByRole(Role.OPERATOR);
    }

    @Override
    public String issueToken(AuthUserPojo user) {
        return JwtHelper.createToken(user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public AuthUserData decodeToken(String token) throws ApiException {
        try {
            Claims c = JwtHelper.parseClaims(token);
            String email = c.get("email", String.class);
            Role role = Role.valueOf(c.get("role", String.class));
            return new AuthUserData(email, role);
        } catch (Exception e) {
            throw new ApiException("Invalid token");
        }
    }


    private Claims parseClaimsOrThrow(String token) throws ApiException {
        try {
            return JwtHelper.parseClaims(token);
        } catch (JwtException e) {
            throw new ApiException("Invalid token");
        }
    }

    private void ensureEmailUnique(String email) throws ApiException {
        AuthUserPojo existing = dao.findByEmail(email);
        if (existing != null) throw new ApiException("User already exists");
    }
}
