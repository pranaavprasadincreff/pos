package com.increff.invoice.controller;

import com.increff.pos.model.data.ErrorData;
import com.increff.pos.model.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvoiceRestControllerAdvice {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorData> handleApiException(ApiException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorData(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorData> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorData(message));
    }
}
