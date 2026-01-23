package com.increff.pos.controller;

import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.MessageData;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppRestControllerAdvice {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageData> handle(ApiException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getMessage().toLowerCase().contains("service unavailable")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        return ResponseEntity
                .status(status)
                .body(new MessageData(e.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<MessageData> handle(DuplicateKeyException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("A record with this key already exists"));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<MessageData> handle(Throwable e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageData("An internal error occurred"));
    }
}