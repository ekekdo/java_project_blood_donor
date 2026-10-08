package com.bloodconnect.exception;

/**
 * Thrown when credentials are wrong or the account cannot be used.
 *
 * WHY separate from ValidationException: "your email format is invalid" is the
 * user's mistake and should be shown next to the field, whereas "wrong password"
 * should be shown as a single generic message at the top of the form. Telling the
 * user WHICH half was wrong would leak whether an email is registered.
 */
public class AuthenticationException extends BloodAppException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
