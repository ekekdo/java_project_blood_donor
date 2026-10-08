package com.bloodconnect.enums;

/**
 * Whether an account may sign in.
 *
 * WHY this exists at all (and is a separate enum rather than a boolean):
 * An unverified hospital and a suspended donor are DIFFERENT problems with
 * different fixes, so the code must be able to tell the user which one it is
 * instead of printing a generic "account disabled".
 */
public enum AccountStatus {

    ACTIVE("Active"),
    /** Registered but not yet approved; a seeded hospital can start as PENDING. */
    PENDING("Pending verification"),
    SUSPENDED("Suspended by admin"),
    DEACTIVATED("Deactivated");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The single rule the AuthService and AuthFilter both depend on. */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    public static AccountStatus fromString(String value) {
        if (value != null) {
            for (AccountStatus status : values()) {
                if (status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown account status: " + value);
    }
}
