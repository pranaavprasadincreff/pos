package com.increff.invoice.controller;

import com.increff.pos.model.exception.ApiException;
import com.increff.invoice.modal.data.ErrorData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvoiceRestControllerAdvice {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorData> handleApiException(ApiException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorData(e.getMessage()));
    }
}
