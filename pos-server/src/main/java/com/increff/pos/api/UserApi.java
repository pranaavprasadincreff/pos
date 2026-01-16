package com.increff.pos.api;

import com.increff.pos.db.UserPojo;
import com.increff.pos.db.UserUpdatePojo;
import com.increff.pos.exception.ApiException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserApi {
    UserPojo addUser(UserPojo userPojo) throws ApiException;
    UserPojo getUserById(String id) throws ApiException;
    UserPojo getUserByEmail(String email) throws ApiException;
    Page<UserPojo> getAllUsers(int page, int size);
    UserPojo updateUser(UserUpdatePojo userUpdatePojo) throws ApiException;
}