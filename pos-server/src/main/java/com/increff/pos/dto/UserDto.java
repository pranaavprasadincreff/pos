package com.increff.pos.dto;

import com.increff.pos.api.UserApi;
import com.increff.pos.db.UserUpdatePojo;
import com.increff.pos.helper.UserHelper;
import com.increff.pos.model.data.UserData;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.db.UserPojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.UserUpdateForm;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class UserDto {
    private final UserApi userApi;
    public UserDto(UserApi userApi) {
        this.userApi = userApi;
    }

    public UserData createUser(UserForm userForm) throws ApiException {
        ValidationUtil.validateForm(userForm);
        UserPojo userPojo = UserHelper.convertFormToEntity(userForm);
        UserPojo savedUserPojo = userApi.addUser(userPojo);
        return UserHelper.convertFormToDto(savedUserPojo);
    }

    public UserData getById(String id) throws ApiException {
        UserPojo userPojo = userApi.getUserById(id);
        return UserHelper.convertFormToDto(userPojo);
    }

    public UserData getByEmail(String email) throws ApiException {
        UserPojo userPojo = userApi.getUserByEmail(email);
        return UserHelper.convertFormToDto(userPojo);
    }

    public Page<UserData> getAllUsers(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<UserPojo> userPage = userApi.getAllUsers(form.getPage(), form.getSize());
        return userPage.map(UserHelper::convertFormToDto);
    }

    public UserData updateUser(UserUpdateForm userUpdateForm) throws ApiException {
        ValidationUtil.validateUpdateForm(userUpdateForm);
        UserUpdatePojo userUpdatePojo = UserHelper.convertUpdateFormToEntity(userUpdateForm);
        UserPojo updated = userApi.updateUser(userUpdatePojo);
        return UserHelper.convertFormToDto(updated);
    }

} 