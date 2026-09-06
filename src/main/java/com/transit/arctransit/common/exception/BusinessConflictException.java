package com.transit.arctransit.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// business rule conflict
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessConflictException
        extends RuntimeException {

    public BusinessConflictException(
            String message) {

        super(message);
    }
}