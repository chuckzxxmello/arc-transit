package com.transit.arctransit.dispatch;

import com.transit.arctransit.common.exception.BusinessConflictException;

public class DispatchConflictException extends BusinessConflictException {

    private final DispatchRule ruleCode;

    public DispatchConflictException(DispatchRule ruleCode, String message) {
        super(ruleCode.name() + ": " + message);
        this.ruleCode = ruleCode;
    }

    public DispatchRule ruleCode() {
        return ruleCode;
    }
}
