package com.transit.arctransit.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an operation violates a domain business rule.
 * Mapped to 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) {
        super(message);
    }
}
