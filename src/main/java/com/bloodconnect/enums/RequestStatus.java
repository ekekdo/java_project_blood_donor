package com.bloodconnect.enums;

/**
 * Lifecycle of a blood request.
 *
 * WHY a real state machine and not a boolean:
 * A request is genuinely multi-stage (open -> partially fulfilled -> fulfilled,
 * plus cancelled/expired), and a boolean cannot express "half done".
 * Transition rules get enforced in the service layer in a later phase.
 */
public enum RequestStatus {

    OPEN("Open"),
    /** Some but not all units collected. */
    PARTIALLY_FULFILLED("Partially Fulfilled"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** True while the request may still receive donations. */
    public boolean isActive() {
        return this == OPEN || this == PARTIALLY_FULFILLED;
    }

    public static RequestStatus fromString(String value) {
        if (value != null) {
            for (RequestStatus status : values()) {
                if (status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown request status: " + value);
    }
}
