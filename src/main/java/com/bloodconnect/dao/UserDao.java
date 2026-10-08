package com.bloodconnect.dao;

import com.bloodconnect.enums.AccountStatus;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.User;

import java.util.Optional;

/**
 * Persistence contract for the {@code users} table.
 *
 * WHY it extends CrudDao<User, Long> rather than declaring its own methods:
 * create/findById/findAll/update/delete are identical to every other table, so
 * inheriting them removes duplication and guarantees consistent behaviour.
 */
public interface UserDao extends CrudDao<User, Long> {

    /**
     * Login lookup. Returns Optional.empty() when no such email exists.
     *
     * WHY return the User even when the password is wrong: the service needs the
     * stored BCrypt hash to run checkpw(). Returning "true/false" from the DAO
     * would put the hashing library inside the persistence layer.
     */
    Optional<User> findByEmail(String email) throws DatabaseException;

    /**
     * Existence probe used before we even attempt the INSERT, so the common case
     * (already-registered email) gets a clean message instead of a SQL error.
     *
     * The UNIQUE index on users.email is still the real guarantee; this is only a
     * friendlier first pass.
     */
    boolean emailExists(String email) throws DatabaseException;

    /** Activity filter for the admin area in a later phase. */
    long countByStatus(AccountStatus status) throws DatabaseException;

    /** Total user count, used for the admin dashboard later (read live, never faked). */
    long countAll() throws DatabaseException;
}
