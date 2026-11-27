package com.bloodconnect.model;

import com.bloodconnect.enums.Role;

/**
 * A person who needs blood (or a relative needing it on their behalf).
 *
 * WHY PatientUser has almost no extra fields even though blood_requests has
 * patient_name: a patient does not register a donor profile. Their details are
 * captured per request, which is the correct 1-to-many relationship. Keeping this
 * class thin is honest modelling, not laziness.
 *
 * @see User
 */
public class PatientUser extends User {

    private Long userId;

    private String city;
    /** Emergency contact, useful if the patient is unconscious when donating. */
    private String emergencyContact;

    /** Creates an empty patient with the PATIENT role. */
    public PatientUser() {
        setRole(Role.PATIENT);
    }

    /** Creates a fully populated patient with the PATIENT role. */
    public PatientUser(String name, String email, String phone, String city, String emergencyContact) {
        super(name, email, phone, Role.PATIENT);
        this.city = city;
        this.emergencyContact = emergencyContact;
    }

    /** POLYMORPHISM: patients land on the patient area. */
    @Override
    public String getDashboardPath() {
        return "/patient/dashboard";
    }

    /** Returns the owning users.id. */
    public Long getUserId() {
        return userId;
    }

    /** Sets the owning users.id. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** Returns the city. */
    public String getCity() {
        return city;
    }

    /** Sets the city. */
    public void setCity(String city) {
        this.city = city;
    }

    /** Returns the emergency contact. */
    public String getEmergencyContact() {
        return emergencyContact;
    }

    /** Sets the emergency contact. */
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    /** Returns a printable summary without the password hash. */
    @Override
    public String toString() {
        return "PatientUser{userId=" + userId + ", city='" + city + "'}";
    }
}
