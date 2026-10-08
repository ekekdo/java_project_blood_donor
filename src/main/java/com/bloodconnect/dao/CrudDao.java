package com.bloodconnect.dao;

import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract shared by every table in the system.
 *
 * WHY GENERIC (the &lt;T, ID&gt; in the name):
 * Without generics every DAO would re-declare the same five methods with a
 * different type, and List&lt;UserDao&gt; could not hold a HospitalDao. With
 * generics, UserDao, DonorDao and HospitalDao just say which concrete type they
 * specialise, and the compiler does the rest. This is type safety replacing
 * casting.
 *
 * WHY create/update/delete RETURN THE ID (or a boolean) instead of void:
 * MySQL's auto-increment id is only known after the INSERT, and the service layer
 * needs it to write the donors/hospitals child row in the same transaction. A void
 * method would force a second SELECT to recover it.
 *
 * @param &lt;T&gt;  the entity type this DAO manages
 * @param &lt;ID&gt; the type of the primary key
 */
public interface CrudDao<T, ID> {

    /**
     * Inserts a new row and returns the generated primary key.
     *
     * WHY DuplicateEmailException is declared here even though only the users table
     * has a unique email: it is the general "this insert conflicts with a business
     * rule" signal. Declaring it on the interface (rather than only on UserDao)
     * means the caller must handle the conflict, and an implementation that cannot
     * produce one - DonorDaoImpl, HospitalDaoImpl - simply does not declare it,
     * which Java allows because an override may narrow its throws clause.
     *
     * @throws DatabaseException       if the insert fails (connection lost, FK violation, ...)
     * @throws DuplicateEmailException if the row conflicts with a uniqueness rule
     */
    ID create(T entity) throws DatabaseException, DuplicateEmailException;

    /**
     * WHY Optional instead of a nullable T or an exception:
     * "No such id" is an ordinary outcome during a GET, not a programming error,
     * so it should not be signalled with an exception. Optional makes the caller
     * acknowledge the empty case at compile time - and forces .orElseThrow() when
     * absence really is an error, which is how we then raise
     * ResourceNotFoundException at the right layer.
     */
    Optional<T> findById(ID id) throws DatabaseException;

    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing row.
     *
     * WHY it can also throw DuplicateEmailException: an update may change an email
     * to one that already exists, hitting the same UNIQUE index as create().
     *
     * @return true if a row was actually changed, false if the id did not exist
     */
    boolean update(T entity) throws DatabaseException, DuplicateEmailException;

    /**
     * @return true if a row was deleted, false if the id did not exist
     */
    boolean delete(ID id) throws DatabaseException;

    /**
     * Default method: "does this row exist?".
     * WHY default: it is expressible as findById().isPresent(), so every DAO would
     * otherwise write the identical body. A default method in an interface shows we
     * know interfaces can carry shared behaviour, not just signatures.
     */
    default boolean existsById(ID id) throws DatabaseException {
        return findById(id).isPresent();
    }

    /**
     * Convenience for the case where absence IS an error.
     * WHY here: the same "not found" wording should be produced everywhere, so the
     * message is written once.
     */
    default T requireById(ID id, String entityName) throws DatabaseException, ResourceNotFoundException {
        return findById(id).orElseThrow(() -> ResourceNotFoundException.forId(entityName, id));
    }
}
