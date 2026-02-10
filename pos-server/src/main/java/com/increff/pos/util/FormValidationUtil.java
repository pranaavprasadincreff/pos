package com.increff.pos.util;

import com.increff.pos.model.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public final class FormValidationUtil {

    private FormValidationUtil() {}

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    public static <T> void validate(T form) throws ApiException {
        if (form == null) {
            throw new ApiException("Request body cannot be null");
        }

        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(form);
        if (violations == null || violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));

        throw new ApiException(message);
    }
}
