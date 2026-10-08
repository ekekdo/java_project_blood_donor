package com.bloodconnect.enums;

/**
 * What a donor decided about a blood request.
 *
 * WHY ResponseStatus AND BloodGroup are separate:
 * Many real systems let a donor say "I can give A+ but not right now", which
 * needs two independent facts. Collapsing them into one enum loses information.
 */
public enum ResponseStatus {

    /** Donor volunteered to donate. */
    ACCEPTED("Accepted"),
    /** Donor is willing but cannot come right now. */
    MAYBE("Maybe"),
    DECLINED("Declined"),
    /** Donor has already donated for this request. */
    COMPLETED("Donation Completed");

    private final String displayName;

    ResponseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Only ACCEPTED/COMPLETED count towards fulfilling a request. */
    public boolean isPositive() {
        return this == ACCEPTED || this == COMPLETED;
    }

    public static ResponseStatus fromString(String value) {
        if (value != null) {
            for (ResponseStatus status : values()) {
                if (status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown response status: " + value);
    }
}
