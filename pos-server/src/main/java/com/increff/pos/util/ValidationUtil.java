package com.increff.pos.util;

import com.increff.pos.exception.ApiException;
import com.increff.pos.model.form.*;
import org.springframework.util.StringUtils;

public class ValidationUtil {
    public static void validateUserForm(UserForm form) throws ApiException {
        validateEmail(form.getEmail());
        validateName(form.getName());
    }

    public static void validateUserUpdateForm(UserUpdateForm form) throws ApiException {
        validateEmail(form.getOldEmail());
        validateEmail(form.getNewEmail());
        validateName(form.getName());
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

    public static void validateProductForm(ProductForm form) throws ApiException {
        validateProductCommon(
                form.getBarcode(),
                form.getClientId(),
                form.getName(),
                form.getMrp()
        );
    }

    public static void validateProductUpdateForm(ProductUpdateForm form) throws ApiException {
        if (!StringUtils.hasText(form.getOldBarcode())) {
            throw new ApiException("Old barcode cannot be empty");
        }
        validateProductCommon(
                form.getNewBarcode(),
                form.getClientId(),
                form.getName(),
                form.getMrp()
        );
    }

    private static void validateProductCommon(
            String barcode,
            String clientId,
            String name,
            Double mrp
    ) throws ApiException {

        if (!StringUtils.hasText(barcode)) {
            throw new ApiException("Barcode cannot be empty");
        }
        if (!StringUtils.hasText(clientId)) {
            throw new ApiException("Client is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new ApiException("Product name cannot be empty");
        }
        if (mrp == null || mrp <= 0) {
            throw new ApiException("Invalid MRP");
        }
    }

    public static void validateInventoryUpdateForm(InventoryUpdateForm form) throws ApiException {
        if (!StringUtils.hasText(form.getProductId())) {
            throw new ApiException("Product id cannot be empty");
        }
        if (form.getQuantity() == null || form.getQuantity() < 0) {
            throw new ApiException("Invalid inventory quantity");
        }
    }

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
