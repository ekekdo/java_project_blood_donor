package com.bloodconnect.model;

import com.bloodconnect.enums.Role;

/**
 * Creates the correct User subclass for a given Role.
 *
 * WHY THIS CLASS EXISTS (the interesting design decision in the DAO layer):
 * A DAO reads one flat users row, but the app needs a polymorphic object. The
 * factory is the SINGLE place that maps Role -> concrete class. Without it, every
 * DAO row-mapper would need its own if/else on role, and adding a role would mean
 * hunting down all of them.
 *
 * WHY a static method and not a Map&lt;Role, Supplier&lt;User&gt;&gt;:
 * A switch on an enum is compile-time exhaustive, so the compiler flags any new
 * Role we forget to handle. It also gives an obvious place to write the
 * IllegalStateException for a role we genuinely cannot build.
 */
public final class UserFactory {

    /** Utility class: not instantiable. */
    private UserFactory() {
        throw new AssertionError("UserFactory is a utility class and must not be instantiated");
    }

    public static User create(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Cannot create a user without a role");
        }
        // Switch on an enum, not a String: the compiler enforces exhaustiveness.
        return switch (role) {
            case DONOR -> new DonorUser();
            case HOSPITAL -> new HospitalUser();
            case PATIENT -> new PatientUser();
            case ADMIN -> new AdminUser();
        };
    }
}
