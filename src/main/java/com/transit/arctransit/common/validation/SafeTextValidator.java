package com.transit.arctransit.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeTextValidator implements ConstraintValidator<SafeText, String> {
    private boolean multiline;

    @Override
    public void initialize(SafeText constraint) {
        multiline = constraint.multiline();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.codePoints().allMatch(code ->
                (!Character.isISOControl(code) || multiline && (code == '\n' || code == '\r' || code == '\t'))
                        && !(code >= Character.MIN_SURROGATE && code <= Character.MAX_SURROGATE));
    }
}
