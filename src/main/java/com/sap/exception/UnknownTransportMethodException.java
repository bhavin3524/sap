package com.sap.exception;

import org.springframework.http.HttpStatus;

public class UnknownTransportMethodException extends ApiException {
    public UnknownTransportMethodException(String message) { super(message, HttpStatus.BAD_REQUEST); }
}
