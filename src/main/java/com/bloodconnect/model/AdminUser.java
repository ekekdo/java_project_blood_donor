package com.bloodconnect.model;

import com.bloodconnect.enums.Role;

/**
 * The system administrator.
 *
 * WHY AdminUser exists even though it has no extra fields: it is the fourth branch
 * of the polymorphism. It proves getDashboardPath() is a real abstraction and not
 * just a switch statement written four times. The class earns its place by being
 * the *only* way to reach the admin area, and it is seeded via sample_data.sql
 * rather than the register page.
 *
 * @see User
 */
public class AdminUser extends User {

    public AdminUser() {
        setRole(Role.ADMIN);
    }

    public AdminUser(String name, String email, String phone) {
        super(name, email, phone, Role.ADMIN);
    }

    /** POLYMORPHISM: the fourth and final override. */
    @Override
    public String getDashboardPath() {
        return "/admin/dashboard";
    }
}
