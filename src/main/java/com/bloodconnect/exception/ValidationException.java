package com.bloodconnect.exception;

/**
 * Thrown when user input fails a business rule (blank name, weak password, ...).
 *
 * WHY it carries the offending FIELD NAME:
 * So register.jsp can attach the message to the exact Bootstrap input using JSTL
 * instead of dumping everything into one generic banner.
 */
public class ValidationException extends BloodAppException {

    private static final long serialVersionUID = 1L;

    /** Which form field the message belongs to, e.g. "email" or "password". */
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
