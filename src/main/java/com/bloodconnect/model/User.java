package com.bloodconnect.model;

import com.bloodconnect.enums.AccountStatus;
import com.bloodconnect.enums.Role;

import java.time.LocalDateTime;

/**
 * Abstract base class for everyone who can sign in.
 *
 * WHY ABSTRACT: DONOR, HOSPITAL, PATIENT and ADMIN all share these seven fields.
 * Duplicating them in four classes would be copy-paste, and a change (say, adding
 * createdAt) would need four edits. "is-a User" is a real inheritance relationship,
 * so we model it as one.
 *
 * WHY THE FIELDS ARE private: encapsulation. Subclasses must not be able to change
 * passwordHash or id directly - they go through getters/setters, which is the one
 * place we can later add validation (e.g. reject a blank email).
 *
 * @see DonorUser
 * @see HospitalUser
 * @see PatientUser
 * @see AdminUser
 */
public abstract class User {

    private Long id;
    private String name;
    private String email;
    private String phone;
    /** BCrypt hash, never the plain password. The plain text is not stored anywhere. */
    private String passwordHash;
    private Role role;
    private AccountStatus status;
    private LocalDateTime createdAt;

    /**
     * POLYMORPHISM ANCHOR POINT.
     *
     * The servlet calls user.getDashboardPath() on a plain User reference, and the
     * JVM picks the subclass override at runtime. That single line in LoginServlet
     * is the whole point of this hierarchy: adding a fifth role later needs no
     * change to the servlet, only a new subclass.
     */
    public abstract String getDashboardPath();

    protected User() {
        // Required by the DAOs, which build instances reflectively-style by
        // setting fields through setters.
    }

    protected User(String name, String email, String phone, Role role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = AccountStatus.ACTIVE;
    }

    // ---- Standard getters/setters (encapsulation) ----

    /** Returns the primary key. */
    public Long getId() {
        return id;
    }

    /** Sets the primary key. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Returns the full name. */
    public String getName() {
        return name;
    }

    /** Sets the full name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the email address. */
    public String getEmail() {
        return email;
    }

    /** Sets the email address. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Returns the phone number. */
    public String getPhone() {
        return phone;
    }

    /** Sets the phone number. */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Returns the BCrypt password hash. */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** Sets the BCrypt password hash. */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Returns the role. */
    public Role getRole() {
        return role;
    }

    /** Sets the role. */
    public void setRole(Role role) {
        this.role = role;
    }

    /** Returns the account status. */
    public AccountStatus getStatus() {
        return status;
    }

    /** Sets the account status. */
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    /** Returns when the account was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Sets when the account was created. */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Overridden so debug logging can never accidentally print a password hash.
     * WHY: a plain default toString() prints every field, which is how hashes end
     * up in production log files.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name='" + name
                + "', email='" + email + "', role=" + role + ", status=" + status + "}";
    }
}
