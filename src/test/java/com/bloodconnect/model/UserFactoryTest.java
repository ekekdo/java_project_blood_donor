package com.bloodconnect.model;

import com.bloodconnect.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFactoryTest {

    @Test
    void donorRoleCreatesDonorUser() {
        User user = UserFactory.create(Role.DONOR);

        assertTrue(user instanceof DonorUser);
        assertEquals(Role.DONOR, user.getRole());
        assertEquals("/donor/dashboard", user.getDashboardPath());
    }

    @Test
    void hospitalRoleCreatesHospitalUser() {
        User user = UserFactory.create(Role.HOSPITAL);

        assertTrue(user instanceof HospitalUser);
        assertEquals("/hospital/dashboard", user.getDashboardPath());
    }

    @Test
    void patientRoleCreatesPatientUser() {
        User user = UserFactory.create(Role.PATIENT);

        assertTrue(user instanceof PatientUser);
        assertEquals("/patient/dashboard", user.getDashboardPath());
    }

    @Test
    void adminRoleCreatesAdminUser() {
        User user = UserFactory.create(Role.ADMIN);

        assertTrue(user instanceof AdminUser);
        assertEquals("/admin/dashboard", user.getDashboardPath());
    }

    @Test
    void nullRoleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFactory.create(null));
    }
}
