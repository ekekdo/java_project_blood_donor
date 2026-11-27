package com.bloodconnect.model;

import com.bloodconnect.enums.Role;

/**
 * A registered hospital / blood bank.
 *
 * WHY 'verified' matters: business rule 2 says unverified hospitals cannot create
 * blood requests, so the flag has to live on the model where the service can read it.
 *
 * @see User
 */
public class HospitalUser extends User {

    private Long userId;

    private String hospitalName;
    /** Government registration number; unique in the DB so two banks cannot share one. */
    private String registrationNo;
    private String address;
    private String city;
    private String contactNumber;

    /** Name of the staff member authorised to raise requests - accountability. */
    private String authorizedPerson;

    /** Business rule 2: only verified hospitals may create blood_requests rows. */
    private boolean verified;

    /** Creates an empty hospital with the HOSPITAL role. */
    public HospitalUser() {
        setRole(Role.HOSPITAL);
    }

    /** Creates a fully populated hospital with the HOSPITAL role. */
    public HospitalUser(String name, String email, String phone, String hospitalName,
                        String registrationNo, String address, String city,
                        String contactNumber, String authorizedPerson) {
        super(name, email, phone, Role.HOSPITAL);
        this.hospitalName = hospitalName;
        this.registrationNo = registrationNo;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
        this.authorizedPerson = authorizedPerson;
    }

    /** POLYMORPHISM: hospitals land on the hospital area. */
    @Override
    public String getDashboardPath() {
        return "/hospital/dashboard";
    }

    /** Returns the owning users.id. */
    public Long getUserId() {
        return userId;
    }

    /** Sets the owning users.id. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** Returns the hospital name. */
    public String getHospitalName() {
        return hospitalName;
    }

    /** Sets the hospital name. */
    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    /** Returns the government registration number. */
    public String getRegistrationNo() {
        return registrationNo;
    }

    /** Sets the government registration number. */
    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    /** Returns the address. */
    public String getAddress() {
        return address;
    }

    /** Sets the address. */
    public void setAddress(String address) {
        this.address = address;
    }

    /** Returns the city. */
    public String getCity() {
        return city;
    }

    /** Sets the city. */
    public void setCity(String city) {
        this.city = city;
    }

    /** Returns the contact number. */
    public String getContactNumber() {
        return contactNumber;
    }

    /** Sets the contact number. */
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    /** Returns the authorised staff member. */
    public String getAuthorizedPerson() {
        return authorizedPerson;
    }

    /** Sets the authorised staff member. */
    public void setAuthorizedPerson(String authorizedPerson) {
        this.authorizedPerson = authorizedPerson;
    }

    /** Returns whether the hospital is verified. */
    public boolean isVerified() {
        return verified;
    }

    /** Sets whether the hospital is verified. */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    /** Returns a printable summary without the password hash. */
    @Override
    public String toString() {
        return "HospitalUser{userId=" + userId + ", hospitalName='" + hospitalName
                + "', registrationNo='" + registrationNo + "', city='" + city
                + "', verified=" + verified + "}";
    }
}
