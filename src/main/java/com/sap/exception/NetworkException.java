package com.sap.exception;

import org.springframework.http.HttpStatus;

public class NetworkException extends ApiException {
    public NetworkException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
