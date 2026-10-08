package com.bloodconnect.enums;

/**
 * WHO a person is in the system. Stored in users.role as a VARCHAR.
 *
 * WHY AN ENUM INSTEAD OF A RAW STRING:
 * A typo like "DONAR" in a string field would only surface at runtime, deep in a
 * request. An enum makes the compiler reject it and gives us a closed set that
 * the AuthFilter can switch on safely.
 */
public enum Role {

    DONOR("Donor"),
    HOSPITAL("Hospital"),
    PATIENT("Patient"),
    /** Seeded only through db/sample_data.sql - never offered on the register page. */
    ADMIN("Admin");

    /** Human-readable label, so JSPs never hard-code "DONOR". */
    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Lenient lookup used when reading a VARCHAR from the database.
     * WHY: the column is a VARCHAR, not a MySQL ENUM, so a bad row should
     * produce a clear IllegalArgumentException instead of a NullPointerException.
     */
    public static Role fromDb(String value) {
        if (value != null) {
            for (Role role : values()) {
                if (role.name().equalsIgnoreCase(value.trim())) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("Unknown role in database: " + value);
    }
}
