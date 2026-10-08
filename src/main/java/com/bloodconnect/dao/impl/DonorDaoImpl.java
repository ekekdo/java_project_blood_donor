package com.bloodconnect.dao.impl;

import com.bloodconnect.dao.DonorDao;
import com.bloodconnect.enums.BloodGroup;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.DonorUser;
import com.bloodconnect.util.AbstractBaseDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-JDBC implementation of {@link DonorDao}.
 *
 * WHY blood_group is stored as a VARCHAR like "O+" and NOT as a MySQL ENUM:
 * A MySQL ENUM would be faster but changing it later needs an ALTER TABLE, and
 * filtering by an application enum stored as VARCHAR is done through a
 * PreparedStatement placeholder, so there is no injection risk and no coupling
 * between the DB schema and the Java enum's constant names.
 */
public class DonorDaoImpl extends AbstractBaseDao implements DonorDao {

    private static final String INSERT_SQL =
            "INSERT INTO donors (user_id, age, gender, blood_group, city, address, available, last_donation_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, age, gender, blood_group, city, address, available, last_donation_date "
                    + "FROM donors";

    private static final String SELECT_BY_ID_SQL = SELECT_COLUMNS + " WHERE id = ?";

    private static final String SELECT_BY_USER_SQL = SELECT_COLUMNS + " WHERE user_id = ?";

    private static final String SELECT_ALL_SQL = SELECT_COLUMNS + " ORDER BY id";

    private static final String SELECT_BY_GROUP_SQL =
            SELECT_COLUMNS + " WHERE blood_group = ? ORDER BY city, id";

    private static final String UPDATE_SQL =
            "UPDATE donors SET age = ?, gender = ?, blood_group = ?, city = ?, address = ?, "
                    + "available = ?, last_donation_date = ? WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM donors WHERE id = ?";

    /**
     * THE ELIGIBILITY QUERY - business rule 1 written in SQL.
     *
     * WHY the 90-day window is expressed with DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)
     * instead of computing a date in Java and passing it as a parameter:
     * the database is the clock, so the filter and the row it filters are always in
     * the same time zone, and the whole check happens on the server with an index
     * rather than after pulling every donor into memory.
     *
     * Note the two placeholders: the caller's group goes in as a ? and the CRITERIA
     * are all parameters. No value is concatenated into the SQL text.
     */
    private static final String ELIGIBLE_SQL =
            SELECT_COLUMNS + " WHERE blood_group = ? AND city = ? AND available = TRUE "
                    + "AND (last_donation_date IS NULL "
                    + "     OR last_donation_date < DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)) "
                    + "ORDER BY last_donation_date IS NOT NULL, last_donation_date";

    @Override
    public Long create(DonorUser donor) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    INSERT_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, donor.getUserId());
                setIntOrNull(ps, 2, donor.getAge());
                ps.setString(3, donor.getGender());
                ps.setString(4, donor.getBloodGroup().getLabel());
                ps.setString(5, donor.getCity());
                ps.setString(6, donor.getAddress());
                setBoolean(ps, 7, donor.isAvailable());
                // Deliberately NULL on insert: nobody has donated at sign-up time.
                setLocalDateOrNull(ps, 8, donor.getLastDonationDate());

                ps.executeUpdate();
                return readGeneratedKey(ps);
            }
        });
    }

    @Override
    public java.util.Optional<DonorUser> findById(Long id) throws DatabaseException {
        if (id == null) {
            return java.util.Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID_SQL)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? java.util.Optional.of(mapDonorRow(rs)) : java.util.Optional.empty();
                }
            }
        });
    }

    @Override
    public List<DonorUser> findAll() throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
                 ResultSet rs = ps.executeQuery()) {
                List<DonorUser> donors = new ArrayList<>();
                while (rs.next()) {
                    donors.add(mapDonorRow(rs));
                }
                return donors;
            }
        });
    }

    @Override
    public boolean update(DonorUser donor) throws DatabaseException {
        if (donor == null || donor.getId() == null) {
            throw new DatabaseException("update(DonorUser) requires a donor with an id");
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
                setIntOrNull(ps, 1, donor.getAge());
                ps.setString(2, donor.getGender());
                ps.setString(3, donor.getBloodGroup().getLabel());
                ps.setString(4, donor.getCity());
                ps.setString(5, donor.getAddress());
                setBoolean(ps, 6, donor.isAvailable());
                setLocalDateOrNull(ps, 7, donor.getLastDonationDate());
                ps.setLong(8, donor.getId());
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public boolean delete(Long id) throws DatabaseException {
        if (id == null) {
            return false;
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(DELETE_SQL)) {
                ps.setLong(1, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * WHY this returns null instead of throwing:
     * a patient or admin has no donor row, and that is a normal state of affairs
     * while registering, not an error. Only the service layer knows whether absence
     * should be fatal, and it raises ResourceNotFoundException if it must.
     */
    @Override
    public DonorUser findByUserId(long userId) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_SQL)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapDonorRow(rs) : null;
                }
            }
        });
    }

    @Override
    public List<DonorUser> findEligibleDonors(BloodGroup bloodGroup, String city) throws DatabaseException {
        if (bloodGroup == null || city == null || city.isBlank()) {
            return List.of();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(ELIGIBLE_SQL)) {
                ps.setString(1, bloodGroup.getLabel());
                ps.setString(2, city.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    List<DonorUser> donors = new ArrayList<>();
                    while (rs.next()) {
                        donors.add(mapDonorRow(rs));
                    }
                    return donors;
                }
            }
        });
    }

    @Override
    public List<DonorUser> findByBloodGroup(BloodGroup bloodGroup) throws DatabaseException {
        if (bloodGroup == null) {
            return List.of();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_GROUP_SQL)) {
                ps.setString(1, bloodGroup.getLabel());
                try (ResultSet rs = ps.executeQuery()) {
                    List<DonorUser> donors = new ArrayList<>();
                    while (rs.next()) {
                        donors.add(mapDonorRow(rs));
                    }
                    return donors;
                }
            }
        });
    }

    @Override
    public boolean setAvailability(long donorId, boolean available) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE donors SET available = ? WHERE id = ?")) {
                setBoolean(ps, 1, available);
                ps.setLong(2, donorId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * Part of the phase-3 transaction: after a confirmed donation the donor must
     * become ineligible for 90 days.
     *
     * WHY this takes the date as a parameter instead of using CURRENT_DATE:
     * the caller runs it inside a transaction, so the test can pass a fixed date and
     * the row is written with the same date that went into the donations table.
     * Two different sources of "now" inside one transaction is a classic bug.
     */
    @Override
    public boolean recordDonationDate(long donorId, LocalDate donationDate) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE donors SET last_donation_date = ?, available = TRUE WHERE id = ?")) {
                setLocalDateOrNull(ps, 1, donationDate);
                ps.setLong(2, donorId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public long countAll() throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM donors");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        });
    }

    private DonorUser mapDonorRow(ResultSet rs) throws SQLException {
        DonorUser donor = new DonorUser();
        donor.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        donor.setUserId(rs.wasNull() ? null : userId);

        int age = rs.getInt("age");
        donor.setAge(rs.wasNull() ? null : age);

        donor.setGender(rs.getString("gender"));
        donor.setBloodGroup(BloodGroup.fromDb(rs.getString("blood_group")));
        donor.setCity(rs.getString("city"));
        donor.setAddress(rs.getString("address"));
        donor.setAvailable(mapBoolean(rs, "available"));
        donor.setLastDonationDate(mapLocalDate(rs, "last_donation_date"));
        return donor;
    }
}
