package com.bloodconnect.enums;

/**
 * How soon a blood request is needed. Drives sort order in the donor's feed.
 *
 * WHY AN ORDINAL-FREE ENUM: we use an explicit int instead of ordinal() because
 * ordinal() changes silently if someone reorders the constants, which would
 * corrupt every urgency value already in the database.
 */
public enum UrgencyLevel {

    ROUTINE(1, "Routine"),
    URGENT(2, "Urgent"),
    CRITICAL(3, "Critical - within 6 hours");

    private final int priority;
    private final String displayName;

    UrgencyLevel(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public int getPriority() {
        return priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UrgencyLevel fromString(String value) {
        if (value != null) {
            String v = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            for (UrgencyLevel level : values()) {
                if (level.name().equals(v)) {
                    return level;
                }
            }
        }
        throw new IllegalArgumentException("Unknown urgency level: " + value);
    }
}
