package com.increff.pos.controller;

import com.increff.pos.model.data.MessageData;
import com.increff.pos.model.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppRestControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AppRestControllerAdvice.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageData> handle(ApiException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = e.getMessage() == null ? "Bad request" : e.getMessage();
        if (msg.toLowerCase().contains("service unavailable")) status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new MessageData(msg));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<MessageData> handle(DuplicateKeyException e) {
        log.warn("DuplicateKeyException", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageData("A record with this key already exists"));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<MessageData> handle(Throwable e) {
        // ✅ THIS is what you need right now
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageData("An internal error occurred"));
    }
}
