package com.increff.pos.helper;


import com.increff.pos.db.UserPojo;
import com.increff.pos.db.UserUpdatePojo;
import com.increff.pos.model.data.UserData;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.form.UserUpdateForm;

import java.util.List;
import java.util.stream.Collectors;

public class UserHelper {
    public static UserPojo convertFormToEntity(UserForm dto) {
        UserPojo userPojo = new UserPojo();
        userPojo.setName(dto.getName());
        userPojo.setEmail(dto.getEmail());
        return userPojo;
    }

    public static UserUpdatePojo convertUpdateFormToEntity(UserUpdateForm dto) {
        UserUpdatePojo userUpdatePojo = new UserUpdatePojo();
        userUpdatePojo.setName(dto.getName());
        userUpdatePojo.setOldEmail(dto.getOldEmail());
        userUpdatePojo.setNewEmail(dto.getNewEmail());
        return userUpdatePojo;
    }

    public static List<UserData> convertToUserDataList(List<UserPojo> userPojoDataList) {
        return userPojoDataList.stream().map(UserHelper::convertFormToDto).collect(Collectors.toList());
    }

    public static UserData convertFormToDto(UserPojo userPojo) {
        UserData userData = new UserData();
        userData.setId(userPojo.getId());
        userData.setName(userPojo.getName());
        userData.setEmail(userPojo.getEmail());
        return userData;
    }
}
