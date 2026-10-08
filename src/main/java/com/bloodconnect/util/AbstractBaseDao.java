package com.bloodconnect.util;

import com.bloodconnect.exception.DatabaseException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shared JDBC plumbing for the concrete DAOs.
 *
 * WHY A COMMON SUPERCLASS: every DAO needs the same try/catch-to-DatabaseException
 * wrapper, the same SQLExceptionState -> DuplicateEmailException translation, and
 * the same java.time <-> java.sql date conversion. Copying that into three files
 * would be three chances to get it wrong. AbstractBaseDao holds it once.
 *
 * WHY THE METHODS ARE STATIC: they hold no per-DAO state. A utility class cannot be
 * instantiated, cannot be misused with a null connection, and can be called from
 * inside a lambda. There is nothing here for a subclass to override.
 */
public abstract class AbstractBaseDao {

    /**
     * Protected, not private: an implicit super() call from every subclass
     * constructor must be able to reach it. Still not publicly instantiable because
     * the class is abstract.
     */
    protected AbstractBaseDao() {
    }

    /** MySQL error code for a UNIQUE / PRIMARY KEY constraint violation. */
    protected static final int SQL_STATE_INTEGRITY_VIOLATION = 23000;

    /**
     * Runs a unit of work inside try-with-resources and converts any SQLException
     * into our DatabaseException.
     *
     * WHY a functional callback instead of duplicating try/catch in every method:
     * every DAO method would otherwise be 6 lines of boilerplate around one line of
     * real logic. Writing it once means the resource handling is correct everywhere.
     *
     * WHY there is no separate "void" overload:
     * Java erases generics, so a void version taking SqlWork&lt;Void&gt; would have
     * the SAME bytecode signature as this one and the compiler would reject the pair
     * with "name clash". Methods that have nothing to return simply `return null;`
     * inside the lambda.
     *
     * @param work  receives an already-open Connection
     */
    @FunctionalInterface
    protected interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    protected static <T> T withConnection(SqlWork<T> work) throws DatabaseException {
        // try-with-resources: connection, statement and result set are all closed
        // automatically on both the success and the exception path. No finally blocks.
        try (Connection connection = DBUtil.getConnection()) {
            return work.execute(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Database operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * The escape hatch for the rare DAO method that must REACT to a specific
     * SQLException instead of just reporting it.
     *
     * WHY this is needed: the UNIQUE index on users.email is the real guarantee that
     * an email is unique, so a duplicate can still surface here as SQLState 23000.
     * If withConnection() swallowed the SQLException into a DatabaseException, the
     * DAO could no longer read getSQLState() and the user would see a raw SQL error
     * instead of DuplicateEmailException.
     *
     * The rule: use withConnection() everywhere, and reach for this ONLY when the
     * method must inspect the driver's error code. It is declared as throwing both
     * types so the compiler still forces the caller to handle the unchecked case.
     */
    protected static <T> T withConnectionInspectingSqlException(SqlWork<T> work)
            throws DatabaseException, SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            return work.execute(connection);
        }
        // No catch block on purpose: the SQLException is allowed to propagate so the
        // caller can inspect its SQLState / error code.
    }

    /**
     * Recognises a UNIQUE-constraint failure.
     *
     * WHY we inspect SQLState instead of matching the message text: the message is
     * localised and version-dependent, whereas SQLState "23000" is the SQL standard's
     * class-23 integrity error and is stable across MySQL versions. Error 1062 is
     * MySQL's specific "Duplicate entry" code.
     */
    protected static boolean isDuplicateKey(SQLException e) {
        return e.getSQLState() != null
                && e.getSQLState().startsWith(String.valueOf(SQL_STATE_INTEGRITY_VIOLATION));
    }

    /**
     * Maps a column to an enum, returning null for a NULL column.
     *
     * WHY allow null: blood_requests.hospital_id is genuinely NULL for a request
     * raised by a patient, so a mapper that assumed non-null would crash.
     */
    protected static <E extends Enum<E>> E mapEnum(Class<E> type, String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(columnValue.trim())) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value in database: " + columnValue);
    }

    /**
     * MySQL TINYINT(1) is not a BOOLEAN, so available / verified / is_read are
     * read as int and converted. is_read is nullable, hence the Boolean overload.
     */
    protected static boolean mapBoolean(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getInt(columnLabel) != 0;
    }

    protected static Boolean mapNullableBoolean(ResultSet rs, String columnLabel) throws SQLException {
        int value = rs.getInt(columnLabel);
        return rs.wasNull() ? null : value != 0;
    }

    /** Null-safe LocalDate read; a NULL DATE becomes null, not an exception. */
    protected static LocalDate mapLocalDate(ResultSet rs, String columnLabel) throws SQLException {
        Date date = rs.getDate(columnLabel);
        return date == null ? null : date.toLocalDate();
    }

    protected static LocalDateTime mapLocalDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel);
        return ts == null ? null : ts.toLocalDateTime();
    }

    // ---- Setters that turn null into SQL NULL ----
    // WHY bother: PreparedStatement.setString(i, null) actually works, but setInt
    // and setBoolean do not accept null, so routing every nullable column through
    // these helpers keeps the DAOs readable and avoids "must not be null" errors.

    protected static void setStringOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        ps.setString(index, value);
    }

    protected static void setIntOrNull(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    protected static void setBoolean(PreparedStatement ps, int index, boolean value) throws SQLException {
        ps.setBoolean(index, value);
    }

    protected static void setLocalDateOrNull(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    /**
     * Closes a statement quietly during cleanup.
     *
     * WHY catch and ignore: a close() failure on an already-broken connection is
     * not interesting, and letting it propagate would mask the REAL error we are
     * already in the middle of reporting.
     */
    protected static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Intentionally ignored - see the comment above.
            }
        }
    }

    /**
     * Reads the auto-generated key that MySQL assigned to the inserted row.
     *
     * WHY this is essential: the donors/hospitals child row needs the users.id
     * that only MySQL knows after the INSERT, and re-SELECTing it by email would be
     * both slower and racy under concurrent registrations.
     */
    protected static long readGeneratedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("INSERT succeeded but no generated key was returned");
    }

    protected static void closeStatement(Statement statement) {
        closeQuietly(statement);
    }
}
