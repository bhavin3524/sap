package com.sap.exception;

import org.springframework.http.HttpStatus;

public class CityNotFoundException extends ApiException {
    public CityNotFoundException(String message) { super(message, HttpStatus.NOT_FOUND); }
}