package com.bloodconnect.service;

import com.bloodconnect.enums.Role;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.User;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void validDonorRegistrationHashesPasswordAndPassesProfileToTransaction() throws Exception {
        TestDoubles.FakeUserDao userDao = new TestDoubles.FakeUserDao();
        TestDoubles.FakeRegistrationDao registrationDao = new TestDoubles.FakeRegistrationDao();
        AuthService service = new AuthService(userDao, new TestDoubles.FakeHospitalDao(), registrationDao);

        User registered = service.register(validDonorForm());

        assertEquals(100L, registered.getId());
        assertNotNull(registered.getPasswordHash());
        assertTrue(registered.getPasswordHash().startsWith("$2"));
        assertNotNull(registrationDao.getDonor());
        assertEquals("Mumbai", registrationDao.getDonor().getCity());
        assertNull(registrationDao.getPatient());
    }

    @Test
    void validPatientRegistrationPassesCityAndEmergencyContactToTransaction() throws Exception {
        TestDoubles.FakeRegistrationDao registrationDao = new TestDoubles.FakeRegistrationDao();
        AuthService service = new AuthService(new TestDoubles.FakeUserDao(),
                new TestDoubles.FakeHospitalDao(), registrationDao);

        AuthService.RegisterForm form = validPatientForm();
        service.register(form);

        assertNotNull(registrationDao.getPatient());
        assertEquals("Pune", registrationDao.getPatient().getCity());
        assertEquals("9876500000", registrationDao.getPatient().getEmergencyContact());
    }

    @Test
    void adminSelfRegistrationIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setRole(Role.ADMIN);

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("role", exception.getField());
    }

    @Test
    void blankNameIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setName("   ");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("name", exception.getField());
    }

    @Test
    void malformedEmailIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setEmail("not-an-email");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("email", exception.getField());
    }

    @Test
    void invalidIndianMobileIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setPhone("1234567890");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("phone", exception.getField());
    }

    @Test
    void weakPasswordIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setPassword("short");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("password", exception.getField());
    }

    @Test
    void missingRoleIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validPatientForm();
        form.setRole(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("role", exception.getField());
    }

    @Test
    void duplicateEmailIsRejectedBeforeTransaction() {
        TestDoubles.FakeUserDao userDao = new TestDoubles.FakeUserDao();
        userDao.setEmailExists(true);
        TestDoubles.FakeRegistrationDao registrationDao = new TestDoubles.FakeRegistrationDao();
        AuthService service = new AuthService(userDao, new TestDoubles.FakeHospitalDao(), registrationDao);

        assertThrows(DuplicateEmailException.class,
                () -> service.register(validPatientForm()));
        assertNull(registrationDao.getUser());
    }

    @Test
    void outOfRangeDonorAgeIsRejected() {
        AuthService service = serviceWithFakes();
        AuthService.RegisterForm form = validDonorForm();
        form.setAge(17);

        ValidationException exception = assertThrows(ValidationException.class, () -> service.register(form));

        assertEquals("age", exception.getField());
    }

    @Test
    void duplicateHospitalRegistrationNumberIsRejectedBeforeTransaction() {
        TestDoubles.FakeHospitalDao hospitalDao = new TestDoubles.FakeHospitalDao();
        hospitalDao.setRegistrationNumberExists(true);
        TestDoubles.FakeRegistrationDao registrationDao = new TestDoubles.FakeRegistrationDao();
        AuthService service = new AuthService(new TestDoubles.FakeUserDao(), hospitalDao, registrationDao);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.register(validHospitalForm()));

        assertEquals("registrationNo", exception.getField());
        assertNull(registrationDao.getUser());
    }

    @Test
    void bcryptHashCanBeVerifiedAndWrongPasswordFails() {
        String hash = BCrypt.hashpw("Secret123", BCrypt.gensalt(4));

        assertTrue(BCrypt.checkpw("Secret123", hash));
        assertFalse(BCrypt.checkpw("Wrong123", hash));
    }

    private AuthService serviceWithFakes() {
        return new AuthService(new TestDoubles.FakeUserDao(), new TestDoubles.FakeHospitalDao(),
                new TestDoubles.FakeRegistrationDao());
    }

    private AuthService.RegisterForm validDonorForm() {
        AuthService.RegisterForm form = baseForm();
        form.setRole(Role.DONOR);
        form.setAge(25);
        form.setGender("Male");
        form.setBloodGroup("O+");
        form.setCity("Mumbai");
        return form;
    }

    private AuthService.RegisterForm validPatientForm() {
        AuthService.RegisterForm form = baseForm();
        form.setRole(Role.PATIENT);
        form.setCity("Pune");
        form.setEmergencyContact("9876500000");
        return form;
    }

    private AuthService.RegisterForm validHospitalForm() {
        AuthService.RegisterForm form = baseForm();
        form.setRole(Role.HOSPITAL);
        form.setCity("Delhi");
        form.setHospitalName("City Hospital");
        form.setRegistrationNo("REG-123");
        form.setAddress("Main Road");
        form.setHospitalPhone("9876543211");
        form.setAuthorizedPerson("Reception Lead");
        return form;
    }

    private AuthService.RegisterForm baseForm() {
        AuthService.RegisterForm form = new AuthService.RegisterForm();
        form.setName("Test User");
        form.setEmail("test.user@example.in");
        form.setPhone("9876543210");
        form.setPassword("Pass@123");
        return form;
    }
}
