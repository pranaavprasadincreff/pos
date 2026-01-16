package com.increff.pos.util;

import com.increff.pos.exception.ApiException;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.UserUpdateForm;
import org.springframework.util.StringUtils;

public class ValidationUtil {
    public static void validateForm(UserForm form) throws ApiException {
        validateEmail(form.getEmail());
        validateName(form.getName());
    }

    public static void validateUpdateForm(UserUpdateForm form) throws ApiException {
        validateEmail(form.getOldEmail());
        validateEmail(form.getNewEmail());
        validateName(form.getName());
    }

    private static void validateId(String id) throws ApiException {
        if (!StringUtils.hasText(id)) {
            throw new ApiException("User id is required for update");
        }
    }

    private static void validateEmail(String email) throws ApiException {
        if (!StringUtils.hasText(email)) {
            throw new ApiException("Email cannot be empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ApiException("Invalid email format");
        }
    }

    private static void validateName(String name) throws ApiException {
        if (!StringUtils.hasText(name)) {
            throw new ApiException("Name cannot be empty");
        }
    }

    // Pagination validations
    public static void validatePageForm(PageForm form) throws ApiException {
        if (form.getPage() < 0) {
            throw new ApiException("Page number cannot be negative");
        }
        if (form.getSize() <= 0) {
            throw new ApiException("Page size must be positive");
        }
        if (form.getSize() > 100) {
            throw new ApiException("Page size cannot be greater than 100");
        }
    }
}