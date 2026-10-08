package com.bloodconnect.util;

import com.bloodconnect.enums.Role;
import com.bloodconnect.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * The ONLY class that reads or writes session attributes.
 *
 * WHY CENTRALISE THIS: the session key name is a magic string. If LoginServlet
 * stores userId under "user" and AuthFilter looks for "loggedInUser", the app
 * appears to work for a moment and then silently treats everyone as logged out.
 * One constant, one class, no such bug.
 *
 * WHY WE STORE ONLY THE userId AND NOT THE WHOLE User OBJECT:
 * The User object is mutable and would become stale the moment somebody updates a
 * row. Storing the id means every request can re-read fresh data, and the session
 * stays small - Tomcat serialises session attributes when it persists or clusters.
 */
public final class SessionUtil {

    /** Session key holding the logged-in user's primary key. */
    public static final String SESSION_USER_ID = "bloodConnectUserId";

    /** Session key holding the role, so AuthFilter can check it without a DB hit. */
    public static final String SESSION_USER_ROLE = "bloodConnectUserRole";

    /**
     * Session key holding the display name, purely so the navbar can greet the user
     * without a database read on every page.
     */
    public static final String SESSION_USER_NAME = "bloodConnectUserName";

    /**
     * Session key holding the result of user.getDashboardPath().
     *
     * WHY cache the polymorphic result in the session instead of rebuilding the URL
     * in the JSP: the path is decided by the concrete subclass, so caching it is the
     * natural consequence of the design. The alternative - assembling
     * "/${lowercase(role)}/dashboard" inside the navbar - would mean the VIEW layer
     * re-implements the role-to-path rule, and the two could drift apart. Reading
     * the value the model layer already computed keeps one source of truth.
     */
    public static final String SESSION_DASHBOARD_PATH = "bloodConnectDashboardPath";

    /** Session key for a one-shot flash message shown after a redirect. */
    public static final String SESSION_FLASH = "bloodConnectFlash";

    private SessionUtil() {
        throw new AssertionError("SessionUtil is a utility class and must not be instantiated");
    }

    /**
     * Logs a user in, destroying any previous session first.
     *
     * WHY SESSION FIXATION PROTECTION: an attacker can plant a known session id in
     * a victim's browser (e.g. via a link). If we reused that session after login,
     * the attacker's id would become an authenticated session. Calling
     * HttpSession.invalidate() and then creating a brand-new session with a
     * different id closes that hole. This is the single most important reason login
     * is not just "set a session attribute".
     */
    public static void establishSession(HttpServletRequest request, User user) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_ROLE, user.getRole().name());
        session.setAttribute(SESSION_USER_NAME, user.getName());
        // getDashboardPath() is called ONCE here, on the abstract User reference.
        // Which implementation runs is decided by the runtime type, which is the
        // whole point of the polymorphism - see User.getDashboardPath().
        session.setAttribute(SESSION_DASHBOARD_PATH, user.getDashboardPath());
    }

    /** Reads the logged-in user's id, or null if nobody is signed in. */
    public static Long getLoggedInUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        // The cast is unchecked because a hostile client cannot set session
        // attributes directly - the container owns them. getAttribute is documented
        // to return Object, so the cast is unavoidable.
        Object value = session.getAttribute(SESSION_USER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    public static Role getLoggedInRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ROLE);
        if (value == null) {
            return null;
        }
        try {
            return Role.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            // A role that no longer exists in the enum: treat as logged out rather
            // than crashing the filter.
            return null;
        }
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getLoggedInUserId(request) != null;
    }

    /** Ends the session completely. Called by LogoutServlet. */
    public static void destroySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Stores a one-shot message that survives exactly one redirect.
     *
     * WHY a flash message instead of a request attribute: after a POST we redirect
     * (POST-Redirect-GET) so a refresh cannot resubmit the form. A plain request
     * attribute would be gone by then, because the redirect is a brand-new request.
     */
    public static void setFlash(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(SESSION_FLASH, message);
    }

    /**
     * Reads and REMOVES the flash message.
     *
     * WHY it is removed: "one-shot" is the whole contract. If it stayed, the message
     * would reappear on every subsequent page load.
     */
    public static String consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_FLASH);
        if (value != null) {
            session.removeAttribute(SESSION_FLASH);
        }
        return value == null ? null : value.toString();
    }
}
