package com.bloodconnect.dao;

import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.HospitalUser;

import java.util.List;

/**
 * Persistence contract for the {@code hospitals} table.
 */
public interface HospitalDao extends CrudDao<HospitalUser, Long> {

    HospitalUser findByUserId(long userId) throws DatabaseException;

    /**
     * WHY this lookup exists as a separate method: business rule 2 says unverified
     * hospitals cannot create requests, and the registration number is the
     * registration_no UNIQUE index. Checking it early gives a clear message
     * instead of a constraint violation three layers down.
     */
    boolean registrationNoExists(String registrationNo) throws DatabaseException;

    List<HospitalUser> findByCity(String city) throws DatabaseException;

    /** Admin action in a later phase; until then it proves the UPDATE path works. */
    boolean setVerified(long hospitalId, boolean verified) throws DatabaseException;

    long countAll() throws DatabaseException;
}
