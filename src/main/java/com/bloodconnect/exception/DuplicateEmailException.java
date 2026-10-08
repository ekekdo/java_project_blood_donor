package com.bloodconnect.exception;

/**
 * Thrown when registering an email that the UNIQUE constraint on users.email rejects.
 *
 * WHY it is distinct from a generic ValidationException: the database is the
 * authority on uniqueness, so the DAO detects the SQLState 23000 integrity
 * violation and translates it. The service then re-raises this so the servlet can
 * show a friendly message instead of a raw SQL error.
 */
public class DuplicateEmailException extends BloodAppException {

    private static final long serialVersionUID = 1L;

    private final String email;

    public DuplicateEmailException(String email) {
        super("An account already exists for email: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
