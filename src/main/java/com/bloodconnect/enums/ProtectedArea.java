package com.bloodconnect.enums;

/**
 * A URL area of the application and the single role allowed to enter it.
 *
 * WHY THIS ENUM EXISTS:
 * AuthFilter has to answer "which role may open /donor/... ?" and the answer must
 * never be duplicated. Declaring the mapping as an enum means:
 * - the compiler enforces that every area names a real Role (unlike a String),
 * - the dashboard path in User.getDashboardPath() and the guarded area here are
 *   declared side by side, so a reader can see both halves of the rule at once,
 * - adding a fifth area is one new constant and nothing else changes.
 */
public enum ProtectedArea {

    ADMIN_AREA("admin", Role.ADMIN),
    DONOR_AREA("donor", Role.DONOR),
    PATIENT_AREA("patient", Role.PATIENT),
    HOSPITAL_AREA("hospital", Role.HOSPITAL);

    /** The first path segment after the context path, e.g. "donor" in /donor/x. */
    private final String pathSegment;

    /** The one role allowed in. */
    private final Role requiredRole;

    ProtectedArea(String pathSegment, Role requiredRole) {
        this.pathSegment = pathSegment;
        this.requiredRole = requiredRole;
    }

    public String getPathSegment() {
        return pathSegment;
    }

    public Role getRequiredRole() {
        return requiredRole;
    }
}
