package com.bloodconnect.dao.impl;

import com.bloodconnect.dao.HospitalDao;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.HospitalUser;
import com.bloodconnect.util.AbstractBaseDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-JDBC implementation of {@link HospitalDao}.
 */
public class HospitalDaoImpl extends AbstractBaseDao implements HospitalDao {

    private static final String INSERT_SQL =
            "INSERT INTO hospitals (user_id, hospital_name, registration_no, address, city, "
                    + "contact_number, authorized_person, verified) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, hospital_name, registration_no, address, city, "
                    + "contact_number, authorized_person, verified FROM hospitals";

    private static final String SELECT_BY_ID_SQL = SELECT_COLUMNS + " WHERE id = ?";

    private static final String SELECT_BY_USER_SQL = SELECT_COLUMNS + " WHERE user_id = ?";

    private static final String SELECT_BY_CITY_SQL = SELECT_COLUMNS + " WHERE city = ? ORDER BY hospital_name";

    private static final String SELECT_ALL_SQL = SELECT_COLUMNS + " ORDER BY city, hospital_name";

    private static final String UPDATE_SQL =
            "UPDATE hospitals SET hospital_name = ?, registration_no = ?, address = ?, city = ?, "
                    + "contact_number = ?, authorized_person = ?, verified = ? WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM hospitals WHERE id = ?";

    @Override
    public Long create(HospitalUser hospital) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    INSERT_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, hospital.getUserId());
                ps.setString(2, hospital.getHospitalName());
                ps.setString(3, hospital.getRegistrationNo());
                ps.setString(4, hospital.getAddress());
                ps.setString(5, hospital.getCity());
                ps.setString(6, hospital.getContactNumber());
                ps.setString(7, hospital.getAuthorizedPerson());
                // Business rule 2: self-registered hospitals start UNVERIFIED. Only an
                // admin can flip this, so a stranger cannot immediately raise requests.
                setBoolean(ps, 8, false);

                ps.executeUpdate();
                return readGeneratedKey(ps);
            }
        });
    }

    @Override
    public java.util.Optional<HospitalUser> findById(Long id) throws DatabaseException {
        if (id == null) {
            return java.util.Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID_SQL)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? java.util.Optional.of(mapHospitalRow(rs)) : java.util.Optional.empty();
                }
            }
        });
    }

    @Override
    public List<HospitalUser> findAll() throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
                 ResultSet rs = ps.executeQuery()) {
                List<HospitalUser> hospitals = new ArrayList<>();
                while (rs.next()) {
                    hospitals.add(mapHospitalRow(rs));
                }
                return hospitals;
            }
        });
    }

    @Override
    public boolean update(HospitalUser hospital) throws DatabaseException {
        if (hospital == null || hospital.getId() == null) {
            throw new DatabaseException("update(HospitalUser) requires a hospital with an id");
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
                ps.setString(1, hospital.getHospitalName());
                ps.setString(2, hospital.getRegistrationNo());
                ps.setString(3, hospital.getAddress());
                ps.setString(4, hospital.getCity());
                ps.setString(5, hospital.getContactNumber());
                ps.setString(6, hospital.getAuthorizedPerson());
                setBoolean(ps, 7, hospital.isVerified());
                ps.setLong(8, hospital.getId());
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

    /** WHY nullable return: a brand-new user has no hospital profile yet. */
    @Override
    public HospitalUser findByUserId(long userId) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_SQL)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapHospitalRow(rs) : null;
                }
            }
        });
    }

    @Override
    public boolean registrationNoExists(String registrationNo) throws DatabaseException {
        if (registrationNo == null || registrationNo.isBlank()) {
            return false;
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM hospitals WHERE registration_no = ? LIMIT 1")) {
                ps.setString(1, registrationNo.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public List<HospitalUser> findByCity(String city) throws DatabaseException {
        if (city == null || city.isBlank()) {
            return List.of();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_CITY_SQL)) {
                ps.setString(1, city.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    List<HospitalUser> hospitals = new ArrayList<>();
                    while (rs.next()) {
                        hospitals.add(mapHospitalRow(rs));
                    }
                    return hospitals;
                }
            }
        });
    }

    /**
     * The admin "approve hospital" action. Business rule 2 depends on this value.
     */
    @Override
    public boolean setVerified(long hospitalId, boolean verified) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE hospitals SET verified = ? WHERE id = ?")) {
                setBoolean(ps, 1, verified);
                ps.setLong(2, hospitalId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public long countAll() throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM hospitals");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        });
    }

    private HospitalUser mapHospitalRow(ResultSet rs) throws SQLException {
        HospitalUser hospital = new HospitalUser();
        hospital.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        hospital.setUserId(rs.wasNull() ? null : userId);
        hospital.setHospitalName(rs.getString("hospital_name"));
        hospital.setRegistrationNo(rs.getString("registration_no"));
        hospital.setAddress(rs.getString("address"));
        hospital.setCity(rs.getString("city"));
        hospital.setContactNumber(rs.getString("contact_number"));
        hospital.setAuthorizedPerson(rs.getString("authorized_person"));
        hospital.setVerified(mapBoolean(rs, "verified"));
        return hospital;
    }
}
