package com.bloodconnect.dao;

import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.model.DonorUser;
import com.bloodconnect.model.HospitalUser;
import com.bloodconnect.model.PatientUser;
import com.bloodconnect.model.User;

/**
 * Writes a new account and its role profile in one database transaction.
 */
public interface RegistrationDao {

    /**
     * Inserts the user row and exactly one matching role-profile row atomically.
     *
     * @param user     the login row to insert
     * @param donor    the donor profile, or null
     * @param hospital the hospital profile, or null
     * @param patient  the patient profile, or null
     * @return the generated users.id
     * @throws DatabaseException       if any insert or transaction command fails
     * @throws DuplicateEmailException if users.email violates its unique index
     */
    Long register(User user, DonorUser donor, HospitalUser hospital, PatientUser patient)
            throws DatabaseException, DuplicateEmailException;
}
