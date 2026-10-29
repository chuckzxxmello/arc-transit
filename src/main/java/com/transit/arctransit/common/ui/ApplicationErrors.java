package com.transit.arctransit.common.ui;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import java.util.stream.Collectors;

public final class ApplicationErrors {
    private ApplicationErrors() {
    }

    public static String message(Throwable error) {
        if (error instanceof ConstraintViolationException validation) {
            String message = validation.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage()).distinct().sorted().collect(Collectors.joining(" "));
            return message.isBlank() ? "Please check the form fields." : message;
        }
        if (error instanceof CommandValidationException || error instanceof BusinessConflictException
                || error instanceof ResourceNotFoundException) {
            return error.getMessage() == null ? "Please check the form fields." : error.getMessage();
        }
        if (error instanceof AccessDeniedException) {
            return "You do not have permission to perform this action.";
        }
        if (error instanceof ObjectOptimisticLockingFailureException) {
            return "This record was changed by another user. Refresh and try again.";
        }
        if (error instanceof DataIntegrityViolationException) {
            LoggerFactory.getLogger(ApplicationErrors.class).warn("A database constraint rejected an operation.", error);
            return "The record conflicts with existing data. Refresh and check your input.";
        }
        LoggerFactory.getLogger(ApplicationErrors.class).error("Application operation failed.", error);
        return "The operation could not be completed. Please try again.";
    }

    public static void show(Throwable error) {
        Notification.show(message(error), 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
