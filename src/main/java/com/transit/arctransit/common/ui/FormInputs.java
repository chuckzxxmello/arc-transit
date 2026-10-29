package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

public final class FormInputs {
    private FormInputs() {
    }

    public static TextField text(TextField field, int limit) {
        field.setMaxLength(limit);
        field.addBlurListener(event -> {
            boolean missing = field.isRequiredIndicatorVisible() && field.getValue().isBlank();
            field.setErrorMessage(missing ? field.getLabel() + " is required."
                    : "Use at most " + limit + " characters.");
            field.setInvalid(missing || field.getValue().length() > limit);
        });
        return field;
    }

    public static TextField recordId(TextField field) {
        text(field, 19);
        field.setAllowedCharPattern("[0-9]");
        field.setPattern("[1-9][0-9]{0,18}");
        field.setErrorMessage("Record ID must be a positive integer.");
        field.addBlurListener(event -> validateRecordId(field));
        return field;
    }

    public static TextField mobileNumber(TextField field) {
        field.setMaxLength(11);
        field.setAllowedCharPattern("[0-9]");
        field.setPattern("09[0-9]{9}");
        field.setPlaceholder("09691477344");
        field.setHelperText("Optional. Use 11 digits starting with 09, for example 09691477344.");
        field.getElement().setAttribute("inputmode", "tel");
        field.addBlurListener(event -> validateMobileNumber(field));
        return field;
    }

    private static boolean validateMobileNumber(TextField field) {
        boolean valid = field.isEmpty() || field.getValue().matches("09[0-9]{9}");
        field.setErrorMessage("Use an 11-digit mobile number starting with 09, such as 09691477344.");
        field.setInvalid(!valid);
        return valid;
    }

    private static boolean validateRecordId(TextField field) {
        boolean invalid = false;
        try {
            invalid = !field.isEmpty() && Long.parseLong(field.getValue()) <= 0;
        } catch (NumberFormatException exception) {
            invalid = true;
        }
        invalid |= field.isRequiredIndicatorVisible() && field.isEmpty();
        field.setErrorMessage("Record ID must be a positive integer.");
        field.setInvalid(invalid);
        return !invalid;
    }

    public static boolean validate(Component... components) {
        boolean valid = true;
        for (Component component : components) {
            valid = validateComponent(component) && valid;
        }
        if (!valid) {
            com.vaadin.flow.component.notification.Notification.show("Please check the highlighted fields.");
        }
        return valid;
    }

    private static boolean validateComponent(Component component) {
        boolean valid = true;
        if (component instanceof TextField field) {
            if ("09[0-9]{9}".equals(field.getPattern())) {
                valid = validateMobileNumber(field);
            } else if ("[1-9][0-9]{0,18}".equals(field.getPattern())) {
                valid = validateRecordId(field);
            } else {
                valid = !(field.isRequiredIndicatorVisible() && field.getValue().isBlank())
                        && field.getValue().length() <= field.getMaxLength();
                field.setErrorMessage(field.isRequiredIndicatorVisible() && field.getValue().isBlank()
                        ? field.getLabel() + " is required." : "Use at most " + field.getMaxLength() + " characters.");
                field.setInvalid(!valid);
            }
        } else if (component instanceof TextArea field) {
            valid = !(field.isRequiredIndicatorVisible() && field.getValue().isBlank())
                    && field.getValue().length() <= field.getMaxLength();
            field.setErrorMessage(field.getLabel() + " must contain "
                    + (field.isRequiredIndicatorVisible() ? "1 to " : "at most ") + field.getMaxLength() + " characters.");
            field.setInvalid(!valid);
        } else if (component instanceof ComboBox<?> field) {
            valid = !field.isRequiredIndicatorVisible() || !field.isEmpty();
            field.setErrorMessage("Select a valid " + field.getLabel().toLowerCase(java.util.Locale.ROOT) + ".");
            field.setInvalid(!valid);
        } else if (component instanceof IntegerField field) {
            valid = field.getValue() == null ? !field.isRequiredIndicatorVisible()
                    : field.getValue() >= field.getMin() && field.getValue() <= field.getMax();
            field.setInvalid(!valid);
        }
        if (component instanceof com.vaadin.flow.component.datepicker.DatePicker field) {
            valid = field.getValue() != null || !field.isRequiredIndicatorVisible() && !field.isInvalid();
            field.setInvalid(!valid);
        } else if (component instanceof com.vaadin.flow.component.timepicker.TimePicker field) {
            valid = field.getValue() != null || !field.isRequiredIndicatorVisible() && !field.isInvalid();
            field.setInvalid(!valid);
        } else if (component instanceof com.vaadin.flow.component.datetimepicker.DateTimePicker field) {
            valid = field.getValue() != null || !field.isRequiredIndicatorVisible() && !field.isInvalid();
            field.setInvalid(!valid);
        }
        if (component instanceof com.vaadin.flow.component.HasValue<?, ?>) {
            return valid;
        }
        for (Component child : component.getChildren().toList()) {
            valid = validateComponent(child) && valid;
        }
        return valid;
    }

    public static TextArea notes(TextArea field) {
        field.setMaxLength(500);
        field.setHelperText("Maximum 500 characters.");
        return field;
    }

    public static <T> ComboBox<T> choice(ComboBox<T> field) {
        field.setAllowCustomValue(false);
        field.addBlurListener(event -> {
            field.setErrorMessage("Select a valid " + field.getLabel().toLowerCase(java.util.Locale.ROOT) + ".");
            field.setInvalid(field.isRequiredIndicatorVisible() && field.isEmpty());
        });
        field.addValueChangeListener(event -> field.setInvalid(
                field.isRequiredIndicatorVisible() && field.isEmpty()));
        field.addAttachListener(event -> field.getElement().executeJs(
                "Promise.resolve(this.updateComplete).then(() => { if (this.inputElement) this.inputElement.maxLength = 100; })"));
        return field;
    }

    public static IntegerField integer(IntegerField field, int minimum, int maximum) {
        field.setMin(minimum);
        field.setMax(maximum);
        field.setErrorMessage("Enter a whole number from " + minimum + " to " + maximum + ".");
        return field;
    }
}
