package com.increff.pos.util;

import com.increff.pos.model.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FormValidator {

    @Autowired
    private Validator validator;

    public <T> void validate(T form) throws ApiException {
        if (form == null) {
            throw new ApiException("Request body cannot be null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(form);
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
