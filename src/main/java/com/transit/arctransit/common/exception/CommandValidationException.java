package com.transit.arctransit.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// invalid command or business input
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CommandValidationException
        extends RuntimeException {

    public CommandValidationException(
            String message) {

        super(message);
    }
}