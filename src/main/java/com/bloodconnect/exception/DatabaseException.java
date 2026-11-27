package com.bloodconnect.exception;

/**
 * Thrown when the database layer fails (SQLException, connection refused, etc).
 *
 * WHY this exists: DAO interfaces declare domain methods. If SQLException leaked
 * into services and servlets, every layer would be coupled to JDBC and the
 * "swap the persistence layer" story would be false. Wrapping it in a custom type
 * keeps the JDBC details inside the dao.impl package.
 */
public class DatabaseException extends BloodAppException {

    private static final long serialVersionUID = 1L;

    /** Creates a database failure with a message and underlying cause. */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Creates a database failure with the given message. */
    public DatabaseException(String message) {
        super(message);
    }
}
