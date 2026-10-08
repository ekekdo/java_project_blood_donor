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

    public PatientUser() {
        setRole(Role.PATIENT);
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    @Override
    public String toString() {
        return "PatientUser{userId=" + userId + ", city='" + city + "'}";
    }
}
