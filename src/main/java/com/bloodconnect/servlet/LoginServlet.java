package com.bloodconnect.servlet;

import com.bloodconnect.exception.AuthenticationException;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.User;
import com.bloodconnect.service.AuthService;
import com.bloodconnect.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles POST /login and GET /logout is handled by LogoutServlet.
 *
 * THE MOST IMPORTANT 15 LINES IN THE PROJECT: LoginServlet calls
 * user.getDashboardPath() on a plain User reference. Which concrete method runs is
 * decided at runtime by the object's actual class (DonorUser, HospitalUser,
 * PatientUser, AdminUser). No if/else on role, and adding a role later touches only
 * the new subclass.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    private final AuthService authService = AuthService.createDefault();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (SessionUtil.isLoggedIn(request)) {
            try {
                User already = authService.findUserById(SessionUtil.getLoggedInUserId(request));
                if (already != null) {
                    // Reuses getDashboardPath() here too, for the same reason.
                    response.sendRedirect(request.getContextPath() + already.getDashboardPath());
                    return;
                }
                // The session points at a user id that no longer exists in the database
                // (the account was deleted, or the sample data was reloaded). Clearing
                // the session is the only safe response - otherwise the stale id would
                // be re-read on every page load and AuthFilter would keep letting the
                // ghost through.
                SessionUtil.destroySession(request);
            } catch (DatabaseException e) {
                LOGGER.log(Level.SEVERE, "Database failure while re-checking the session", e);
                SessionUtil.destroySession(request);
            }
        }
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // null-initialised so the catch blocks can always render the form again, and
        // so a failure while READING the parameters is also caught and logged below.
        String email = null;

        try {
            email = request.getParameter("email");
            String password = request.getParameter("password");

            User user = authService.login(email, password);

            // Old session invalidated, brand-new one created: session fixation defence.
            SessionUtil.establishSession(request, user);

            LOGGER.log(Level.INFO, "Login successful for user id {0} ({1})", new Object[]{user.getId(), user.getRole()});

            SessionUtil.setFlash(request, "Signed in as " + user.getName());
            // POLYMORPHIC DISPATCH - no switch, no if/else chain on role.
            response.sendRedirect(request.getContextPath() + user.getDashboardPath());

        } catch (AuthenticationException e) {
            // Bad credentials, disabled account, or an unverified hospital. All three
            // are the user's problem to fix, so we go back to the login form with a
            // message rather than throwing.
            request.setAttribute("errorMessage", e.getMessage());
            // Keeps the email in the field so the user only retypes the password.
            request.setAttribute("enteredEmail", email == null ? "" : email);
            // FORWARD, not redirect: login.jsp reads requestScope.errorMessage and
            // requestScope.enteredEmail, which a redirect would throw away - the
            // user would get the bare form back with no explanation at all.
            request.getRequestDispatcher("/login.jsp").forward(request, response);

        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Database failure during login", e);
            request.setAttribute("errorMessage",
                    "The server could not reach the database. Please try again shortly.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);

        } catch (Throwable e) {
            // LAST RESORT for anything unexpected (ExceptionInInitializerError from
            // DBUtil, NPE, an unmappable enum value in a row...): full stack trace to
            // the console, plain 500 page to the browser via error.jsp - which never
            // prints the trace to the user.
            LOGGER.log(Level.SEVERE, "Unexpected failure during login", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
