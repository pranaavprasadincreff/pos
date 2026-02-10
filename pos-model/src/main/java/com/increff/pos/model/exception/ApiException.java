package com.increff.pos.model.exception;

import lombok.Getter;
import java.io.Serial;

@Getter
public class ApiException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;

    public ApiException(String message) {
        super(message);
        this.code = null;
    }

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public boolean hasCode() {
        return code != null;
    }
}
