package com.increff.pos.controller;

import com.increff.pos.model.data.MessageData;
import com.increff.pos.model.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class AppRestControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AppRestControllerAdvice.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageData> handle(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageData(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageData> handle(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageData(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageData> handle(HttpMessageNotReadableException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("Invalid request body"));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageData> handle(ApiException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = exception.getMessage() == null ? "Bad request" : exception.getMessage();
        if (message.toLowerCase().contains("service unavailable")) status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new MessageData(message));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<MessageData> handle(DuplicateKeyException exception) {
        log.warn("DuplicateKeyException", exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("A record with this key already exists"));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<MessageData> handle(Throwable exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageData("An internal error occurred"));
    }
}
