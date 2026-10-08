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

    public HospitalUser() {
        setRole(Role.HOSPITAL);
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAuthorizedPerson() {
        return authorizedPerson;
    }

    public void setAuthorizedPerson(String authorizedPerson) {
        this.authorizedPerson = authorizedPerson;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public String toString() {
        return "HospitalUser{userId=" + userId + ", hospitalName='" + hospitalName
                + "', registrationNo='" + registrationNo + "', city='" + city
                + "', verified=" + verified + "}";
    }
}
