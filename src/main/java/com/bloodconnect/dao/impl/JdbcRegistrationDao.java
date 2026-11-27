package com.bloodconnect.dao.impl;

import com.bloodconnect.dao.RegistrationDao;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.model.DonorUser;
import com.bloodconnect.model.HospitalUser;
import com.bloodconnect.model.PatientUser;
import com.bloodconnect.model.User;
import com.bloodconnect.util.AbstractBaseDao;
import com.bloodconnect.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link RegistrationDao}.
 *
 * <p>One connection is opened, auto-commit is disabled, the user and role-profile
 * inserts are executed, then the whole unit is committed. Any failure rolls the
 * inserts back so a user row can never exist without its role-profile row.</p>
 */
public class JdbcRegistrationDao extends AbstractBaseDao implements RegistrationDao {

    private static final Logger LOGGER = Logger.getLogger(JdbcRegistrationDao.class.getName());

    private static final String INSERT_USER_SQL = """
            INSERT INTO users (name, email, phone, password_hash, role, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_DONOR_SQL = """
            INSERT INTO donors (user_id, age, gender, blood_group, city, address, available, last_donation_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_HOSPITAL_SQL = """
            INSERT INTO hospitals (user_id, hospital_name, registration_no, address, city,
                                   contact_number, authorized_person, verified)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_PATIENT_SQL = """
            INSERT INTO patients (user_id, city, emergency_contact)
            VALUES (?, ?, ?)
            """;

    /**
     * Registers a user and the single matching profile row in one transaction.
     *
     * @param user     the login row to insert
     * @param donor    the donor profile, or null
     * @param hospital the hospital profile, or null
     * @param patient  the patient profile, or null
     * @return the generated users.id
     * @throws DatabaseException       if any insert or transaction command fails
     * @throws DuplicateEmailException if users.email violates its unique index
     */
    @Override
    public Long register(User user, DonorUser donor, HospitalUser hospital, PatientUser patient)
            throws DatabaseException, DuplicateEmailException {
        boolean originalAutoCommit = true;
        try (Connection connection = DBUtil.getConnection()) {
            try {
                originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                Long userId = insertUser(connection, user);
                if (donor != null) {
                    donor.setUserId(userId);
                    donor.setId(insertDonor(connection, donor));
                } else if (hospital != null) {
                    hospital.setUserId(userId);
                    hospital.setId(insertHospital(connection, hospital));
                } else if (patient != null) {
                    patient.setUserId(userId);
                    patient.setId(insertPatient(connection, patient));
                }

                connection.commit();
                return userId;
            } catch (SQLException e) {
                rollback(connection);
                throw new DatabaseException("Could not complete registration: " + e.getMessage(), e);
            } catch (DuplicateEmailException | RuntimeException e) {
                rollback(connection);
                throw e;
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not open a database connection for registration: "
                    + e.getMessage(), e);
        }
    }

    private Long insertUser(Connection connection, User user) throws SQLException, DuplicateEmailException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_USER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, user.getPasswordHash());
            statement.setString(5, user.getRole().name());
            statement.setString(6, user.getStatus().name());
            statement.executeUpdate();
            return readGeneratedKey(statement);
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateEmailException(user.getEmail());
            }
            throw e;
        }
    }

    private Long insertDonor(Connection connection, DonorUser donor) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_DONOR_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, donor.getUserId());
            setIntOrNull(statement, 2, donor.getAge());
            statement.setString(3, donor.getGender());
            statement.setString(4, donor.getBloodGroup().getLabel());
            statement.setString(5, donor.getCity());
            statement.setString(6, donor.getAddress());
            setBoolean(statement, 7, donor.isAvailable());
            setLocalDateOrNull(statement, 8, donor.getLastDonationDate());
            statement.executeUpdate();
            return readGeneratedKey(statement);
        }
    }

    private Long insertHospital(Connection connection, HospitalUser hospital) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_HOSPITAL_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, hospital.getUserId());
            statement.setString(2, hospital.getHospitalName());
            statement.setString(3, hospital.getRegistrationNo());
            statement.setString(4, hospital.getAddress());
            statement.setString(5, hospital.getCity());
            statement.setString(6, hospital.getContactNumber());
            statement.setString(7, hospital.getAuthorizedPerson());
            setBoolean(statement, 8, false);
            statement.executeUpdate();
            return readGeneratedKey(statement);
        }
    }

    private Long insertPatient(Connection connection, PatientUser patient) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_PATIENT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, patient.getUserId());
            setStringOrNull(statement, 2, patient.getCity());
            setStringOrNull(statement, 3, patient.getEmergencyContact());
            statement.executeUpdate();
            return readGeneratedKey(statement);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            LOGGER.log(Level.WARNING, "Registration rollback failed", rollbackFailure);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean originalAutoCommit) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreFailure) {
            LOGGER.log(Level.WARNING, "Could not restore the original auto-commit mode", restoreFailure);
        }
    }
}
