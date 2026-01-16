package com.increff.pos.controller;

import com.increff.pos.model.data.UserData;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.dto.UserDto;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.form.UserUpdateForm;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;

import java.util.List;

@Tag(name = "User Management", description = "APIs for managing users")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserDto userDto;
    public UserController(UserDto userDto) {
        this.userDto = userDto;
    }

    @Operation(summary = "Create a new user")
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    public UserData createUser(@RequestBody UserForm userForm) throws ApiException {
        return userDto.createUser(userForm);
    }

    @Operation(summary = "Get all users with pagination")
    @RequestMapping(path = "/get-all-paginated", method = RequestMethod.POST)
    public Page<UserData> getAllUsers(@RequestBody PageForm form) throws ApiException {
        return userDto.getAllUsers(form);
    }

    @Operation(summary = "Get user by ID")
    @RequestMapping(path = "/get-by-email/{email}", method = RequestMethod.GET)
    public UserData getByEmail(@PathVariable String email) throws ApiException {
        return userDto.getByEmail(email);
    }

    @Operation(summary = "Update existing user")
    @RequestMapping(path = "/update", method = RequestMethod.POST)
    public UserData updateUser(@RequestBody UserUpdateForm userUpdateForm) throws ApiException {
        return userDto.updateUser(userUpdateForm);
    }
}
