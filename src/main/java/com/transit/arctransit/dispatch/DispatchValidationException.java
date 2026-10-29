package com.transit.arctransit.dispatch;

import com.transit.arctransit.common.exception.CommandValidationException;

public class DispatchValidationException extends CommandValidationException {

    private final DispatchRule ruleCode;

    public DispatchValidationException(DispatchRule ruleCode, String message) {
        super(ruleCode.name() + ": " + message);
        this.ruleCode = ruleCode;
    }

    public DispatchRule ruleCode() {
        return ruleCode;
    }
}
