package com.bloodconnect.servlet;

import com.bloodconnect.enums.Role;
import com.bloodconnect.exception.AuthenticationException;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.exception.ValidationException;
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
 * Handles POST /register.
 *
 * WHY @WebServlet ANNOTATION INSTEAD OF web.xml:
 * Both are valid Servlet 3.0+ and the annotation keeps the mapping next to the class
 * that implements it, so there is no separate file to keep in sync. web.xml still
 * exists in this project for welcome-file, error-page and JSP settings, which
 * annotations cannot express.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());

    /**
     * WHY an instance field and not a local variable in doPost:
     * A servlet instance is created ONCE and reused for every request on that URL.
     * Keeping the service here means the DAO objects are created once instead of on
     * every form submission. This is safe because AuthService is stateless - it holds
     * no per-user data, so there is nothing to be shared incorrectly between the
     * threads that Tomcat uses to serve requests.
     */
    private final AuthService authService = AuthService.createDefault();

    /**
     * A GET should show the form, not try to register. Sending a GET to a
     * state-changing URL is a classic mistake (crawlers, prefetchers); refusing it
     * loudly is better than quietly doing nothing.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SessionUtil.isLoggedIn(request)) {
            // No reason to register twice.
            response.sendRedirect(request.getContextPath() + "/login?error=alreadyLoggedIn");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Inside the try: even a failure while READING the parameters must land in
        // the catch-all below (logged + error.jsp), not escape doPost unlogged.
        AuthService.RegisterForm form = new AuthService.RegisterForm();

        try {
            form = extractForm(request);
            User user = authService.register(form);

            // Registration does NOT sign the user in automatically for a hospital,
            // because a pending account cannot be used. Everyone else is signed in
            // and sent to the dashboard chosen by POLYMORPHISM.
            if (user.getRole() == Role.HOSPITAL) {
                SessionUtil.setFlash(request,
                        "Registration received. An administrator must verify your hospital "
                                + "before you can sign in.");
                response.sendRedirect(request.getContextPath() + "/login?registered=1");
                return;
            }

            SessionUtil.establishSession(request, user);
            SessionUtil.setFlash(request, "Welcome, " + user.getName() + ". Your account is ready.");
            // THE POLYMORPHISM PAYOFF: one line, no switch on role. The concrete
            // subclass decides where this user belongs.
            response.sendRedirect(request.getContextPath() + user.getDashboardPath());

        } catch (ValidationException e) {
            // A field-level problem: go back to the form with the message attached to
            // the offending input via the 'field' attribute on the exception.
            LOGGER.log(Level.FINE, "Registration rejected: " + e.getMessage());
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("errorField", e.getField());
            // Sticky form: keep what the user typed so they do not retype everything.
            request.setAttribute("form", form);
            // FORWARD, not redirect: register.jsp reads requestScope.errorMessage /
            // requestScope.form, and a redirect starts a brand-new request that
            // would drop them - the user would see the bare form with no message.
            request.getRequestDispatcher("/register.jsp").forward(request, response);

        } catch (DuplicateEmailException e) {
            LOGGER.log(Level.INFO, "Duplicate registration attempt for email: " + e.getEmail());
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("errorField", "email");
            request.setAttribute("form", form);
            request.getRequestDispatcher("/register.jsp").forward(request, response);

        } catch (DatabaseException e) {
            // Log the real cause on the server; show the user something neutral.
            // WHY: stack traces leak schema details to attackers, but a student demo
            // still needs the trace in the console to debug it.
            LOGGER.log(Level.SEVERE, "Database failure during registration", e);
            request.setAttribute("errorMessage",
                    "We could not complete your registration because of a database problem. "
                            + "Please try again in a moment.");
            request.setAttribute("form", form);
            request.getRequestDispatcher("/register.jsp").forward(request, response);

        } catch (Throwable e) {
            // LAST RESORT: anything the three blocks above do not cover (an
            // ExceptionInInitializerError from DBUtil, a NullPointerException, an
            // unknown enum value in a row...). Log the FULL stack trace to the
            // console first, then let the container forward to error.jsp, which
            // shows a plain message and never prints the trace to the browser.
            LOGGER.log(Level.SEVERE, "Unexpected failure during registration", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Copies request parameters into the form bean.
     *
     * WHY each field is read defensively: getParameter returns null when a field is
     * absent (unchecked checkboxes, a crafted request, a disabled field), and
     * Integer.parseInt on a missing value would throw NumberFormatException and
     * produce a 500 page instead of a validation message.
     */
    private AuthService.RegisterForm extractForm(HttpServletRequest request) {
        AuthService.RegisterForm form = new AuthService.RegisterForm();

        form.setName(request.getParameter("name"));
        form.setEmail(request.getParameter("email"));
        form.setPhone(request.getParameter("phone"));
        form.setPassword(request.getParameter("password"));
        form.setCity(request.getParameter("city"));
        form.setAddress(request.getParameter("address"));
        form.setEmergencyContact(request.getParameter("emergencyContact"));

        // Donor-only
        form.setAge(parseIntOrNull(request.getParameter("age")));
        form.setGender(request.getParameter("gender"));
        form.setBloodGroup(request.getParameter("bloodGroup"));

        // Hospital-only
        form.setHospitalName(request.getParameter("hospitalName"));
        form.setRegistrationNo(request.getParameter("registrationNo"));
        form.setHospitalPhone(request.getParameter("hospitalPhone"));
        form.setAuthorizedPerson(request.getParameter("authorizedPerson"));

        // Role: null is a valid outcome (unchecked radio buttons) and AuthService
        // turns it into "Please choose an account type".
        String roleParam = request.getParameter("role");
        if (roleParam != null && !roleParam.isBlank()) {
            try {
                form.setRole(Role.valueOf(roleParam.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                form.setRole(null);
            }
        }
        return form;
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            // Return null so the service reports "Age is required" with the field
            // name, instead of a stack trace in the browser.
            return null;
        }
    }
}
