package com.increff.pos.controller;

import com.increff.pos.model.data.MessageData;
import com.increff.pos.model.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;

@RestControllerAdvice
public class AppRestControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AppRestControllerAdvice.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageData> handle(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageData(firstFieldErrorMessage(exception)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageData> handle(ConstraintViolationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageData(firstConstraintViolationMessage(exception)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageData> handle(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("Invalid request body"));
    }

    @ExceptionHandler(InvocationTargetException.class)
    public ResponseEntity<MessageData> handle(InvocationTargetException exception) {
        Throwable target = exception.getTargetException();

        if (target instanceof ApiException apiEx) {
            return handle(apiEx);
        }

        if (target instanceof DuplicateKeyException dk) {
            return handle(dk);
        }

        if (target instanceof DataIntegrityViolationException div) {
            return handle(div);
        }

        log.error("InvocationTargetException (unwrapped target not mapped)", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageData("An internal error occurred"));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageData> handle(ApiException exception) {
        HttpStatus status = resolveStatus(exception);
        String message = resolveMessage(exception);
        return ResponseEntity.status(status).body(new MessageData(message));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<MessageData> handle(DuplicateKeyException exception) {
        log.warn("DuplicateKeyException", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("A record with this key already exists"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageData> handle(DataIntegrityViolationException exception) {
        log.warn("DataIntegrityViolationException", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("A record with this key already exists"));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<MessageData> handle(Throwable exception) {
        log.error("Unhandled exception", exception);

        Throwable cause = exception.getCause();
        if (cause instanceof ApiException apiEx) {
            return handle(apiEx);
        }
        if (cause instanceof DuplicateKeyException dk) {
            return handle(dk);
        }
        if (cause instanceof DataIntegrityViolationException div) {
            return handle(div);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageData("An internal error occurred"));
    }

    // -------------------- private helpers --------------------

    private String firstFieldErrorMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError == null) return "Validation failed";
        String message = fieldError.getDefaultMessage();
        return message == null || message.isBlank() ? "Validation failed" : message;
    }

    private String firstConstraintViolationMessage(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        if (violations == null || violations.isEmpty()) return "Validation failed";

        Iterator<ConstraintViolation<?>> iterator = violations.iterator();
        ConstraintViolation<?> violation = iterator.hasNext() ? iterator.next() : null;

        if (violation == null) return "Validation failed";
        String message = violation.getMessage();
        return message == null || message.isBlank() ? "Validation failed" : message;
    }

    private HttpStatus resolveStatus(ApiException exception) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("service unavailable")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolveMessage(ApiException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Bad request" : message;
    }
}
