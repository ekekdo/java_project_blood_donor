package com.bloodconnect.exception;

/**
 * Base of the whole custom exception hierarchy.
 *
 * WHY make it CHECKED (extends Exception, not RuntimeException):
 * A university rubric wants to see that we understand the difference. Checked
 * forces every DAO caller to think about "what if the database is down?", which
 * is exactly the reasoning we want to be able to explain in the viva.
 *
 * WHY it is abstract: you should never be able to throw a bare BloodAppException
 * and lose the information about which layer actually failed.
 */
public abstract class BloodAppException extends Exception {

    private static final long serialVersionUID = 1L;

    protected BloodAppException(String message) {
        super(message);
    }

    protected BloodAppException(String message, Throwable cause) {
        // Passing the cause through keeps the original SQLException stack trace,
        // so we never lose root-cause information when re-wrapping.
        super(message, cause);
    }
}
