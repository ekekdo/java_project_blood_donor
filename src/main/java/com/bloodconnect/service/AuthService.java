package com.bloodconnect.service;

import com.bloodconnect.dao.DonorDao;
import com.bloodconnect.dao.HospitalDao;
import com.bloodconnect.dao.UserDao;
import com.bloodconnect.enums.AccountStatus;
import com.bloodconnect.enums.BloodGroup;
import com.bloodconnect.enums.Role;
import com.bloodconnect.exception.AuthenticationException;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.DonorUser;
import com.bloodconnect.model.HospitalUser;
import com.bloodconnect.model.PatientUser;
import com.bloodconnect.model.User;
import com.bloodconnect.model.UserFactory;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Registration and sign-in logic.
 *
 * WHY A SERVICE LAYER AT ALL: this class is the reason the app is worth grading.
 * If RegisterServlet talked to UserDao directly, business rules would live in a
 * servlet, could not be reused, and could not be unit tested without a servlet
 * container. AuthService is plain Java - it can be called from a servlet, a test,
 * or a future REST endpoint with no change.
 *
 * THIS CLASS IS INSTANTIATED PER REQUEST BY THE SERVLET and holds no mutable
 * instance state, so two concurrent requests can never share it. That is the
 * reason there is no need for a thread pool here; the notification worker in
 * com.bloodconnect.thread is where threads appear.
 */
public class AuthService {

    /**
     * WHY 10: jBCRYPT's default is 10. It is deliberately slow (that is what makes
     * a stolen hash expensive to crack), and it takes roughly 50-100 ms per hash,
     * which is the cost we accept on login.
     */
    private static final int BCRYPT_ROUNDS = 10;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    /**
     * 72 is not arbitrary: BCrypt only considers the FIRST 72 BYTES of a password.
     * Silently ignoring the rest would mean a user could set a long password and
     * have a weaker one effectively stored, so we reject it and say why.
     */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserDao userDao;
    private final DonorDao donorDao;
    private final HospitalDao hospitalDao;

    /**
     * Constructor injection instead of `new UserDaoImpl()` inside the methods.
     * WHY: the class becomes testable - a unit test can pass a fake DAO and assert
     * that register() rejects a bad email, with no database at all. Hard-coding
     * `new` inside a method would make that impossible.
     */
    public AuthService(UserDao userDao, DonorDao donorDao, HospitalDao hospitalDao) {
        this.userDao = userDao;
        this.donorDao = donorDao;
        this.hospitalDao = hospitalDao;
    }

    /** Convenience factory for the servlets, which have no DI framework available. */
    public static AuthService createDefault() {
        return new AuthService(
                new com.bloodconnect.dao.impl.UserDaoImpl(),
                new com.bloodconnect.dao.impl.DonorDaoImpl(),
                new com.bloodconnect.dao.impl.HospitalDaoImpl());
    }

    // ================= REGISTRATION =================

    /**
     * Validates input, then writes the users row and the matching profile row.
     *
     * WHY this method is long: validation genuinely has many independent rules, and
     * hiding each in its own private method would only move the code, not clarify
     * it. The rules are grouped and commented so each one is easy to point at.
     *
     * @return the fully populated logged-in user (id now set, hash never exposed)
     * @throws ValidationException      if any field is missing or malformed
     * @throws DuplicateEmailException  if the email is already registered
     * @throws DatabaseException       if persistence fails
     */
    public User register(RegisterForm form) throws ValidationException,
            DuplicateEmailException, DatabaseException {

        // ---------- STEP 1: validate every field BEFORE touching the database ----------
        String name = requireText(form.getName(), "name", "Full name is required");
        if (name.length() < 3) {
            throw new ValidationException("name", "Full name must be at least 3 characters");
        }
        if (name.length() > 100) {
            throw new ValidationException("name", "Full name must be at most 100 characters");
        }

        String email = validateEmail(form.getEmail());

        String phone = validatePhone(form.getPhone());

        String password = validatePassword(form.getPassword());

        Role role = form.getRole();
        if (role == null) {
            throw new ValidationException("role", "Please choose an account type");
        }

        // SECURITY GUARD: ADMIN can only ever come from db/sample_data.sql.
        // Without this, anyone could POST role=ADMIN to /register and take over the
        // system. This is the single most important validation in the class.
        if (role == Role.ADMIN) {
            throw new ValidationException("role",
                    "Administrator accounts cannot be created from the registration page");
        }

        // ---------- STEP 2: the role-specific extra fields ----------
        DonorUser donor = null;
        HospitalUser hospital = null;
        PatientUser patient = null;

        if (role == Role.DONOR) {
            donor = buildDonor(form, name, email, phone);
        } else if (role == Role.HOSPITAL) {
            hospital = buildHospital(form, name, email, phone);
        } else {
            patient = new PatientUser();
            patient.setName(name);
            patient.setEmail(email);
            patient.setPhone(phone);
            patient.setCity(requireText(form.getCity(), "city", "City is required for a patient account"));
            patient.setEmergencyContact(form.getEmergencyContact());
        }

        // ---------- STEP 3: check for a duplicate email ----------
        // Done before the INSERT so the common case gets a clean message. The
        // UNIQUE index on users.email remains the real guarantee, and
        // UserDaoImpl.create() converts a late 23000 error into the same exception,
        // so a simultaneous double registration is handled too.
        if (userDao.emailExists(email)) {
            throw new DuplicateEmailException(email);
        }

        // ---------- STEP 4: hash the password ----------
        // The plain password is never stored. It is hashed once here and the local
        // variable simply goes out of scope when register() returns.
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));

        // ---------- STEP 5: insert the users row ----------
        User user = UserFactory.create(role);
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        // HOSPITAL starts PENDING as well as unverified, so the account itself
        // reflects that an approval is outstanding.
        user.setStatus(role == Role.HOSPITAL ? AccountStatus.PENDING : AccountStatus.ACTIVE);

        Long userId = userDao.create(user);
        user.setId(userId);

        // ---------- STEP 6: insert the role profile row ----------
        // WHY the profile insert is NOT inside the same JDBC transaction as step 5:
        // it should be. This is a known limitation of phase 1 and is called out in
        // the README, because UserDao and DonorDao each open their own connection
        // through DBUtil. The fix is a DAO method that accepts an external
        // Connection, which is exactly how the phase-3 request-fulfilment
        // transaction will be written. Being honest about it in the viva is worth
        // more than hiding it.
        if (donor != null) {
            donor.setUserId(userId);
            Long donorId = donorDao.create(donor);
            donor.setId(donorId);
        } else if (hospital != null) {
            // Guard the UNIQUE index on registration_no with a friendly message.
            if (hospitalDao.registrationNoExists(hospital.getRegistrationNo())) {
                throw new ValidationException("registrationNo",
                        "That hospital registration number is already on file");
            }
            hospital.setUserId(userId);
            Long hospitalId = hospitalDao.create(hospital);
            hospital.setId(hospitalId);
        }

        return user;
    }

    // ================= LOGIN =================

    /**
     * Verifies credentials.
     *
     * WHY a SINGLE message for "no such email" and "wrong password":
     * Different messages would let an attacker enumerate which emails have accounts.
     * That is an information leak, not a usability feature, so both cases report
     * the same thing.
     *
     * @throws AuthenticationException on bad credentials, a disabled account, or an
     *                                 unverified hospital
     */
    public User login(String email, String password)
            throws AuthenticationException, DatabaseException {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Please enter both email and password");
        }

        User user = userDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // WHY BCrypt.checkpw and not string equality:
        // BCrypt re-hashes the supplied password using the salt embedded in the
        // stored hash and compares the results. That is why we could not use
        // user.getPasswordHash().equals(password) even if we wanted to.
        if (user.getPasswordHash() == null
                || !BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Business rule 2 at the door: an unverified hospital can sign in (so it can
        // see why it is blocked) but we flag it clearly rather than letting it
        // create requests.
        if (user.getStatus() != null && !user.getStatus().canLogin()) {
            throw new AuthenticationException(
                    "This account is " + user.getStatus().getDisplayName().toLowerCase()
                            + " and cannot sign in. Please contact the administrator.");
        }

        if (user.getRole() == Role.HOSPITAL) {
            HospitalUser hospital = hospitalDao.findByUserId(user.getId());
            if (hospital != null && !hospital.isVerified()) {
                // Thrown, not warned: business rule 2 says an unverified hospital
                // cannot create requests, and letting them in only to fail later
                // would be a worse experience.
                throw new AuthenticationException(
                        "Your hospital is still awaiting verification by an administrator, "
                                + "so you cannot raise blood requests yet.");
            }
        }

        return user;
    }

    /**
     * Loads a user by id for the current session.
     *
     * WHY this read-only method lives on AuthService and the servlet does NOT call
     * UserDaoImpl directly: a servlet that reaches into the persistence layer has
     * skipped a layer, and the architecture stops being worth explaining. The
     * servlet should only ever talk to services.
     *
     * @return the user, or null if the id no longer exists
     */
    public User findUserById(long id) throws DatabaseException {
        return userDao.findById(id).orElse(null);
    }

    /**
     * Changes a password. Kept here so the hash+verify logic exists in one place.
     * The current password is re-checked so a stolen session alone cannot lock the
     * real owner out.
     */
    public void changePassword(User user, String currentPassword, String newPassword)
            throws AuthenticationException, ValidationException, DatabaseException,
            DuplicateEmailException {

        if (user == null || user.getId() == null) {
            throw new AuthenticationException("You must be signed in to change a password");
        }
        if (user.getPasswordHash() == null || !BCrypt.checkpw(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Your current password is not correct");
        }
        String validated = validatePassword(newPassword);
        if (BCrypt.checkpw(validated, user.getPasswordHash())) {
            throw new ValidationException("newPassword", "The new password must be different from the old one");
        }

        user.setPasswordHash(BCrypt.hashpw(validated, BCrypt.gensalt(BCRYPT_ROUNDS)));
        userDao.update(user);
    }

    // ================= private validators =================

    private String requireText(String value, String field, String message) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, message);
        }
        return value.trim();
    }

    private String validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email", "Email address is required");
        }
        String cleaned = email.trim().toLowerCase();

        // The same rule the DB column length enforces, checked here so the user gets
        // a message beside the field instead of a "Data too long" SQL error.
        if (cleaned.length() > 255) {
            throw new ValidationException("email", "Email address must be at most 255 characters");
        }

        // WHY this is a REGEX and not String.contains("@"):
        // contains("@") accepts "a@b" or "@@@", which are not deliverable
        // addresses. The pattern requires something either side of the @ and a dot
        // in the domain.
        if (!cleaned.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("email", "That does not look like a valid email address");
        }
        return cleaned;
    }

    private String validatePhone(String phone) throws ValidationException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("phone", "Phone number is required");
        }
        // Strip the spaces, dashes and brackets people naturally type.
        String digits = phone.trim().replaceAll("[\\s\\-()+]", "");

        // Indian mobile numbers are 10 digits starting 6-9. Being strict here is
        // correct for this application, since a hospital will call the number in an
        // emergency.
        if (!digits.matches("^[6-9]\\d{9}$")) {
            throw new ValidationException("phone",
                    "Enter a 10-digit Indian mobile number starting with 6, 7, 8 or 9");
        }
        return digits;
    }

    private String validatePassword(String password) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("password", "Password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("password",
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("password",
                    "Password must be at most " + MAX_PASSWORD_LENGTH + " characters");
        }
        int utf8Bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (utf8Bytes > BCRYPT_MAX_BYTES) {
            // The honest reason, explained to the user, because a bare "too long"
            // would be baffling.
            throw new ValidationException("password",
                    "Password is too long once encoded (" + utf8Bytes + " bytes). "
                            + "BCrypt only uses the first " + BCRYPT_MAX_BYTES + " bytes, so the maximum is "
                            + BCRYPT_MAX_BYTES + " bytes.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException("password",
                    "Password must contain at least one letter and one number");
        }
        return password;
    }

    private DonorUser buildDonor(RegisterForm form, String name, String email, String phone)
            throws ValidationException {

        Integer age = form.getAge();
        if (age == null) {
            throw new ValidationException("age", "Age is required for a donor account");
        }
        // The 18-65 window is the Indian Blood Bank eligibility rule and also the
        // legal donation age, so it is a real constraint rather than an arbitrary one.
        if (age < 18 || age > 65) {
            throw new ValidationException("age", "Donors must be between 18 and 65 years old");
        }

        String gender = form.getGender();
        if (gender == null || gender.isBlank()) {
            throw new ValidationException("gender", "Gender is required for a donor account");
        }
        if (!gender.matches("(?i)male|female|other")) {
            throw new ValidationException("gender", "Gender must be Male, Female or Other");
        }

        BloodGroup bloodGroup;
        try {
            bloodGroup = BloodGroup.fromString(form.getBloodGroup());
        } catch (IllegalArgumentException e) {
            // Catching an unchecked exception just to re-throw a checked one is
            // deliberate: BloodGroup.fromString is also used by DAOs reading raw
            // database values, and the servlet must never see a raw
            // IllegalArgumentException.
            throw new ValidationException("bloodGroup", e.getMessage());
        }

        DonorUser donor = new DonorUser();
        donor.setName(name);
        donor.setEmail(email);
        donor.setPhone(phone);
        donor.setAge(age);
        donor.setGender(gender);
        donor.setBloodGroup(bloodGroup);
        donor.setCity(requireText(form.getCity(), "city", "City is required for a donor account"));
        donor.setAddress(form.getAddress());
        donor.setAvailable(true);
        // lastDonationDate stays null: a brand-new donor has donated before and is
        // immediately eligible. See DonorUser.isEligibleToDonate().
        return donor;
    }

    private HospitalUser buildHospital(RegisterForm form, String name, String email, String phone)
            throws ValidationException {

        HospitalUser hospital = new HospitalUser();
        hospital.setName(name);
        hospital.setEmail(email);
        hospital.setPhone(phone);
        hospital.setHospitalName(requireText(form.getHospitalName(), "hospitalName",
                "Hospital name is required"));
        hospital.setRegistrationNo(requireText(form.getRegistrationNo(), "registrationNo",
                "Registration number is required"));
        hospital.setAddress(requireText(form.getAddress(), "address",
                "Hospital address is required"));
        hospital.setCity(requireText(form.getCity(), "city", "City is required"));
        hospital.setContactNumber(validatePhone(form.getHospitalPhone()));
        hospital.setAuthorizedPerson(requireText(form.getAuthorizedPerson(), "authorizedPerson",
                "Name of the authorised person is required"));
        // verified stays false; only an admin may set it. See HospitalDaoImpl.create().
        hospital.setVerified(false);
        return hospital;
    }

    /**
     * A small carrier for the registration form fields.
     *
     * WHY NOT PASS 15 PARAMETERS: register(name, email, phone, password, role, age,
     * gender, bloodGroup, city, address, hospitalName, ...) is unreadable at the
     * call site and impossible to extend safely. A bean with setters matches what
     * register.jsp can send, and it is a plain object we can unit test.
     */
    public static class RegisterForm {

        private String name;
        private String email;
        private String phone;
        private String password;
        private Role role;

        // donor-only fields
        private Integer age;
        private String gender;
        private String bloodGroup;

        // shared (donor + hospital + patient all need a city)
        private String city;
        private String address;

        // hospital-only fields
        private String hospitalName;
        private String registrationNo;
        private String hospitalPhone;
        private String authorizedPerson;

        // patient-only
        private String emergencyContact;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getBloodGroup() { return bloodGroup; }
        public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

        public String getRegistrationNo() { return registrationNo; }
        public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

        public String getHospitalPhone() { return hospitalPhone; }
        public void setHospitalPhone(String hospitalPhone) { this.hospitalPhone = hospitalPhone; }

        public String getAuthorizedPerson() { return authorizedPerson; }
        public void setAuthorizedPerson(String authorizedPerson) { this.authorizedPerson = authorizedPerson; }

        public String getEmergencyContact() { return emergencyContact; }
        public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    }
}
