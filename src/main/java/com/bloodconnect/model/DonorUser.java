package com.bloodconnect.model;

import com.bloodconnect.enums.BloodGroup;
import com.bloodconnect.enums.Role;

import java.time.LocalDate;

/**
 * A registered donor.
 *
 * WHY A SEPARATE TABLE (donors) INSTEAD OF EXTRA COLUMNS ON users:
 * Only donors have blood group and donation history. Adding age/gender/blood_group
 * to `users` would force every hospital and patient row to carry NULLs for those
 * columns, and the 1-to-1 relationship needs its own table with its own primary key.
 *
 * @see User
 */
public class DonorUser extends User {

    /** The users.id this donor row belongs to (donors.user_id FK). */
    private Long userId;

    private Integer age;
    private String gender;

    private BloodGroup bloodGroup;
    private String city;
    private String address;

    /** Business rule: a donor must be available to receive requests at all. */
    private boolean available;

    /**
     * Business rule: a donor is eligible only if lastDonationDate is null or older
     * than 90 days. Stored as DATE, so it can be compared in SQL later.
     */
    private LocalDate lastDonationDate;

    public DonorUser() {
        setRole(Role.DONOR);
    }

    public DonorUser(String name, String email, String phone, Integer age, String gender,
                     BloodGroup bloodGroup, String city, String address) {
        super(name, email, phone, Role.DONOR);
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.address = address;
        this.available = true;
    }

    /**
     * POLYMORPHISM: donors land on the donor area, which AuthFilter protects with
     * Role.DONOR.
     */
    @Override
    public String getDashboardPath() {
        return "/donor/dashboard";
    }

    /**
     * The 90-day recovery window, as a business rule in one place.
     * WHY not a magic number in the DAO: the rule is then testable, reusable by
     * the notification thread later, and easy to point at during the viva.
     */
    public static final int DONATION_COOLDOWN_DAYS = 90;

    /**
     * Business rule 1 in executable form.
     * WHY 'today' is a parameter: a method that hard-codes LocalDate.now() cannot be
     * unit tested, and this rule is exactly the kind we will want to test.
     */
    public boolean isEligibleToDonate(LocalDate today) {
        if (!available) {
            return false;
        }
        if (lastDonationDate == null) {
            return true;
        }
        return lastDonationDate.plusDays(DONATION_COOLDOWN_DAYS).isBefore(today);
    }

    /** Convenience overload for callers in a request thread. */
    public boolean isEligibleToDonate() {
        return isEligibleToDonate(LocalDate.now());
    }

    // ---- Getters/setters ----

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    @Override
    public String toString() {
        return "DonorUser{userId=" + userId + ", bloodGroup=" + bloodGroup
                + ", city='" + city + "', available=" + available
                + ", lastDonationDate=" + lastDonationDate + "}";
    }
}
