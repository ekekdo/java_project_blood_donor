package com.bloodconnect.util;

import com.bloodconnect.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The ONE place in the project that knows how to open a JDBC connection.
 *
 * WHY a utility class instead of a Connection in every DAO:
 * Connection creation needs a URL, a username and a password. If each DAO loaded
 * them, we would have 4 copies of the same loading code and changing the driver
 * would mean editing 4 files. Centralising it also gives us exactly one class to
 * point at in the viva when asked "how does your app talk to MySQL?".
 *
 * WHY NOT a connection POOL in phase 1:
 * A pool (HikariCP) is the right production answer, but it would hide the JDBC
 * fundamentals the rubric wants to see. DriverManager.getConnection() is the
 * textbook path, and try-with-resources closes the connection deterministically.
 * Adding a pool later is a change inside THIS class only.
 */
public final class DBUtil {

    /** The one loaded config, read once at class-init time. */
    private static final Properties PROPS = new Properties();

    static {
        // Classpath resource: with Maven this resolves to target/classes/db.properties.
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                // Fail loudly and early. A silent null here would otherwise surface
                // as a confusing NullPointerException or a SQLException about a null URL.
                throw new IllegalStateException(
                        "db.properties not found on the classpath. Copy db.properties.example to "
                                + "db.properties in src/main/resources/ and fill in your MySQL password.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Could not read db.properties: " + e.getMessage());
        }

        // Explicit driver load. Since JDBC 4 the driver self-registers via the
        // service loader, but doing it ourselves makes the dependency obvious and
        // turns a typo in db.driver into a clear ClassNotFoundException.
        String driver = PROPS.getProperty("db.driver");
        if (driver == null || driver.isBlank()) {
            throw new IllegalStateException("db.driver is missing from db.properties");
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "JDBC driver not on the classpath: " + driver);
        }
    }

    private DBUtil() {
        throw new AssertionError("DBUtil is a utility class and must not be instantiated");
    }

    /**
     * Hands out a NEW connection every call.
     *
     * WHY this throws DatabaseException (our checked type) and not SQLException:
     * This is the bottom of the stack, so it is where the raw JDBC exception is
     * first translated into the vocabulary the rest of the app speaks. Callers
     * further up never need to import java.sql.
     */
    public static Connection getConnection() throws DatabaseException {
        String url = PROPS.getProperty("db.url");
        String user = PROPS.getProperty("db.username");
        String password = PROPS.getProperty("db.password");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // The message deliberately does NOT include the password.
            throw new DatabaseException(
                    "Could not connect to MySQL. Is the server running and are the "
                            + "db.url / db.username / db.password values in db.properties correct?", e);
        }
    }

    /**
     * Helper that turns a ResultSet cursor into a finished list and closes the
     * statement.
     *
     * WHY this exists: a DAO that forgets to close a ResultSet leaks memory for the
     * life of the webapp, and forgetting is easy when the same three lines are
     * copied five times. One helper means one correct copy.
     *
     * @param mapper  the per-row mapping, supplied by the caller as a lambda
     * @param <T>     the entity type
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }

    /**
     * Reads every row of an already-EXECUTED PreparedStatement.
     *
     * WHY the statement is passed in already executed rather than the SQL text:
     * this helper must never see SQL as a String, or it would become a place where
     * concatenated SQL could be introduced. Keeping the signature
     * PreparedStatement-only makes that mistake impossible here.
     */
    public static <T> T readSingleRow(PreparedStatement ps, RowMapper<T> mapper) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapper.mapRow(rs) : null;
        }
    }

    /** Same idea, but for a query that returns many rows. */
    public static <T> java.util.List<T> readAllRows(PreparedStatement ps, RowMapper<T> mapper)
            throws SQLException {
        java.util.List<T> results = new java.util.ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapper.mapRow(rs));
            }
        }
        return results;
    }
}
