package com.bloodconnect.dao.impl;

import com.bloodconnect.dao.UserDao;
import com.bloodconnect.enums.AccountStatus;
import com.bloodconnect.enums.Role;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.model.User;import com.bloodconnect.model.UserFactory;
import com.bloodconnect.util.AbstractBaseDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Plain-JDBC implementation of {@link UserDao}. Every statement is a
 * PreparedStatement - no SQL is ever built by concatenation anywhere in this class.
 *
 * WHY the SQL text lives in a private static final String:
 * It keeps the statement next to the code that fills its placeholders, and because
 * the field is final the JVM can prepare it once per class. It is still only a
 * TEMPLATE: the ? characters are what make it injection-proof.
 */
public class UserDaoImpl extends AbstractBaseDao implements UserDao {

    /**
     * No user-supplied value is ever appended to these strings. Columns and table
     * names are fixed at compile time; only values travel as ? placeholders.
     */
    private static final String INSERT_SQL =
            "INSERT INTO users (name, email, phone, password_hash, role, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLUMNS =
            "SELECT id, name, email, phone, password_hash, role, status, created_at FROM users";

    private static final String SELECT_BY_ID_SQL = SELECT_COLUMNS + " WHERE id = ?";

    private static final String SELECT_BY_EMAIL_SQL = SELECT_COLUMNS + " WHERE email = ?";

    private static final String SELECT_ALL_SQL = SELECT_COLUMNS + " ORDER BY created_at DESC, id DESC";

    private static final String UPDATE_SQL =
            "UPDATE users SET name = ?, email = ?, phone = ?, password_hash = ?, role = ?, status = ? "
                    + "WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM users WHERE id = ?";

    /**
     * Inserts the user and returns the new auto-increment id.
     *
     * WHY we re-throw a duplicate email as DuplicateEmailException:
     * users.email has a UNIQUE index, so a race between two people registering the
     * same address at the same moment can still slip past the earlier
     * emailExists() check. The database is the final authority, so the DAO
     * translates its 23000 error into a domain exception. Without this, the user
     * would see a raw "Duplicate entry 'x' for key 'users.email'".
     */
    @Override
    public Long create(User user) throws DatabaseException, DuplicateEmailException {
        try {
            return withConnectionInspectingSqlException(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        INSERT_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPhone());
                    ps.setString(4, user.getPasswordHash());
                    ps.setString(5, user.getRole().name());
                    ps.setString(6, user.getStatus() == null
                            ? AccountStatus.ACTIVE.name() : user.getStatus().name());

                    ps.executeUpdate();
                    return readGeneratedKey(ps);
                }
            });
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateEmailException(user.getEmail());
            }
            throw new DatabaseException("Could not create user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) throws DatabaseException {
        if (id == null) {
            return Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID_SQL)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapUserRow(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<User> findByEmail(String email) throws DatabaseException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_EMAIL_SQL)) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapUserRow(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
                 ResultSet rs = ps.executeQuery()) {
                java.util.List<User> users = new java.util.ArrayList<>();
                while (rs.next()) {
                    users.add(mapUserRow(rs));
                }
                return users;
            }
        });
    }

    @Override
    public boolean update(User user) throws DatabaseException, DuplicateEmailException {
        if (user == null || user.getId() == null) {
            // Guard: an UPDATE with WHERE id = NULL silently changes 0 rows, which
            // looks like success. Failing loudly is safer.
            throw new DatabaseException("update(User) requires a user with an id");
        }
        try {
            return withConnectionInspectingSqlException(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPhone());
                    ps.setString(4, user.getPasswordHash());
                    ps.setString(5, user.getRole().name());
                    ps.setString(6, user.getStatus() == null
                            ? AccountStatus.ACTIVE.name() : user.getStatus().name());
                    ps.setLong(7, user.getId());
                    return ps.executeUpdate() == 1;
                }
            });
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateEmailException(user.getEmail());
            }
            throw new DatabaseException("Could not update user " + user.getId() + ": " + e.getMessage(), e);
        }
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

    @Override
    public boolean emailExists(String email) throws DatabaseException {
        if (email == null || email.isBlank()) {
            return false;
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM users WHERE email = ? LIMIT 1")) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public long countByStatus(AccountStatus status) throws DatabaseException {
        if (status == null) {
            return 0L;
        }
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE status = ?")) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    @Override
    public long countAll() throws DatabaseException {
        return withConnection(connection -> {
            // The string "SELECT COUNT(*) FROM users" contains no user input at all,
            // so it is safe to hard-code; a PreparedStatement would only add noise.
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        });
    }

    /**
     * Turns one flat users row into the RIGHT SUBCLASS of User.
     *
     * WHY UserFactory here: this is the moment polymorphism pays off. The DAO reads
     * a row with a role column and produces a DonorUser, HospitalUser, PatientUser
     * or AdminUser automatically. LoginServlet then just calls
     * user.getDashboardPath() without ever knowing the concrete class.
     */
    private User mapUserRow(ResultSet rs) throws SQLException {
        Role role = mapEnum(Role.class, rs.getString("role"));

        User user = UserFactory.create(role);
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setStatus(mapEnum(AccountStatus.class, rs.getString("status")));
        user.setCreatedAt(mapLocalDateTime(rs, "created_at"));
        return user;
    }

    /**
     * Kept so a future admin screen can change just the status without touching
     * the whole row (demonstrates a targeted PreparedStatement).
     */
    public boolean updateStatus(long userId, AccountStatus status) throws DatabaseException {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE users SET status = ? WHERE id = ?")) {
                ps.setString(1, status.name());
                ps.setLong(2, userId);
                return ps.executeUpdate() == 1;
            }
        });
    }
}
