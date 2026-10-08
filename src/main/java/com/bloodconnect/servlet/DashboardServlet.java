package com.bloodconnect.servlet;

import com.bloodconnect.dao.DonorDao;
import com.bloodconnect.dao.HospitalDao;
import com.bloodconnect.dao.UserDao;
import com.bloodconnect.enums.Role;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.User;
import com.bloodconnect.util.SessionUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves the four clean dashboard URLs /admin/dashboard, /donor/dashboard,
 * /patient/dashboard and /hospital/dashboard by loading the signed-in user plus a
 * few live record counts, then forwarding to the matching JSP.
 *
 * WHY A SERVLET AT ALL INSTEAD OF PUTTING /donor/dashboard.jsp DIRECTLY IN THE WEBAPP:
 * A JSP can only put HTML on the screen. Getting the user's name and the record
 * counts requires Java and database access, and the correct place for that is a
 * servlet, which then forwards to a view. A JSP that calls a DAO directly is the
 * classic mistake, because it ends up with database code embedded in HTML.
 *
 * WHY ALL FOUR ARE HANDLED BY ONE CLASS: the work is identical apart from the
 * target JSP. Four near-identical servlets would be four places to fix the same
 * bug.
 *
 * WHY THIS SERVLET TRUSTS THE SESSION: AuthFilter has already proved the visitor
 * holds the right role for this URL, so re-checking it here would be duplicated
 * logic. The servlet's only job is to gather data and forward.
 */
/**
 * WHY EXACT PATTERNS AND NOT "/admin/*":
 * The servlet forwards to /admin/dashboard.jsp. URL-pattern precedence is
 * exact match, then LONGEST PREFIX match, then extension match - so "/admin/*"
 * beats the container's "*.jsp" mapping and the forward is routed straight back
 * into this same servlet. That is an endless doGet -> forward -> doGet loop,
 * which dies with StackOverflowError (or a storm of new JDBC connections) and
 * surfaces as HTTP 500 on error.jsp for EVERY dashboard. Exact patterns still
 * catch the clean dashboard URL, but no longer shadow the JSP. AuthFilter keeps
 * its "/admin/*" pattern, so the guard is unchanged.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {
        "/admin/dashboard",
        "/donor/dashboard",
        "/patient/dashboard",
        "/hospital/dashboard"
})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    private final UserDao userDao = new com.bloodconnect.dao.impl.UserDaoImpl();
    private final DonorDao donorDao = new com.bloodconnect.dao.impl.DonorDaoImpl();
    private final HospitalDao hospitalDao = new com.bloodconnect.dao.impl.HospitalDaoImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // AuthFilter has already established that a session exists. If the session
        // vanished between the filter and here, hand the visitor to the login page
        // rather than throwing a NullPointerException on the unboxing below.
        Long userId = SessionUtil.getLoggedInUserId(request);
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // Map the URL area to the JSP that renders it. This mirrors
        // ProtectedArea, so the area decides the view just as it decided the role.
        String area = extractArea(request.getServletPath());
        String jspPath = "/" + area + "/dashboard.jsp";
        if (!isKnownArea(area)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            User user = userDao.findById(userId).orElse(null);
            if (user == null) {
                // The account was deleted while the session was still alive.
                SessionUtil.destroySession(request);
                response.sendRedirect(request.getContextPath() + "/login.jsp");
                return;
            }

            request.setAttribute("loggedInUser", user);
            request.setAttribute("userCount", userDao.countAll());
            request.setAttribute("donorCount", donorDao.countAll());
            request.setAttribute("hospitalCount", hospitalDao.countAll());
            request.setAttribute("roleTasks", tasksFor(user.getRole()));

            // forward(), not sendRedirect(): a forward reuses the SAME request, so
            // the attributes set above are still available in the JSP. A redirect
            // would start a brand-new request and lose all of them.
            RequestDispatcher dispatcher = request.getRequestDispatcher(jspPath);
            dispatcher.forward(request, response);

        } catch (DatabaseException e) {
            // Log the cause for the developer, show a neutral page for the user.
            LOGGER.log(Level.SEVERE, "Could not load the dashboard for user " + userId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The dashboard data could not be loaded.");

        } catch (Throwable e) {
            // Anything DatabaseException does not cover still gets a full stack trace
            // in the console before the container forwards to error.jsp, so a failure
            // here is debuggable without showing the user any technical detail.
            LOGGER.log(Level.SEVERE, "Unexpected failure loading the dashboard for user " + userId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST is refused on purpose. Nothing here changes data, so a POST can only be
     * a mistake or a stray crawler hit, and answering it with a redirect to the
     * same page is clearer than silently treating it as a GET.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + request.getServletPath());
    }

    private boolean isKnownArea(String area) {
        return switch (area) {
            case "admin", "donor", "patient", "hospital" -> true;
            // Default branch: an unknown area is a 404, never a pass-through. This
            // is the deny-by-default principle, same as BloodCompatibility.
            default -> false;
        };
    }

    /**
     * The role-specific "what you can do here" list.
     *
     * WHY a Map<Role, List<Task>> and not an if/else in the JSP: the copy is content,
     * not layout, and keeping it in Java means it is one place to edit, and it can
     * be described without talking about markup. A switch on an enum is
     * compile-time exhaustive, so adding a fifth role makes the compiler point here.
     */
    private List<Task> tasksFor(Role role) {
        List<Task> tasks = switch (role) {
            case DONOR -> List.of(
                    new Task("Toggle your availability",
                            "sets donors.available. A donor who is not available is excluded from every match."),
                    new Task("See your eligibility",
                            "computed from last_donation_date: you are eligible again once 90 days have passed."),
                    new Task("Respond to nearby requests",
                            "matched on your blood group and your city."));
            case HOSPITAL -> List.of(
                    new Task("Raise a blood request",
                            "only possible while hospitals.verified = 1, which is why unverified accounts cannot sign in."),
                    new Task("Track your requests",
                            "open, partially fulfilled, fulfilled or cancelled."),
                    new Task("Confirm a donation",
                            "the donation, the request status and the donor's next eligible date are written in one transaction."));
            case PATIENT -> List.of(
                    new Task("Request blood",
                            "for yourself or a relative, with blood group, city and required date."),
                    new Task("See who responded",
                            "donors who accepted, may do, or declined."),
                    new Task("Keep an emergency contact",
                            "so a hospital can reach someone quickly."));
            case ADMIN -> List.of(
                    new Task("Verify hospitals",
                            "flips hospitals.verified to 1, which unlocks request creation."),
                    new Task("Manage accounts",
                            "suspend or reactivate through users.status."),
                    new Task("Read the audit log",
                            "every action is written to activity_logs."));
        };
        return tasks;
    }

    /**
     * "/donor/dashboard" -> "donor".
     *
     * WHY not the last path segment: the URL is /donor/dashboard, so the AREA is the
     * first segment and the view is the second. Using substringAfterLast would give
     * "dashboard" and map every role to the wrong JSP.
     */
    private String extractArea(String servletPath) {
        if (servletPath == null || servletPath.isEmpty() || servletPath.equals("/")) {
            return "";
        }
        String relative = servletPath.startsWith("/") ? servletPath.substring(1) : servletPath;
        int slash = relative.indexOf('/');
        return slash == -1 ? relative : relative.substring(0, slash);
    }

    /**
     * One dashboard bullet point.
     *
     * WHY a plain class with getters and NOT a Java record:
     * a record exposes title() / description(), but JSP Expression Language looks
     * for JavaBean conventions - getTitle() / getDescription(). Using a record here
     * would make ${task.title} silently render as empty in the page, which is a very
     * confusing bug to chase.
     *
     * WHY it is public static: EL reaches these values by reflection, so the class
     * and its getters must be visible outside DashboardServlet. A private nested
     * class compiles fine and then fails at render time.
     */
    public static final class Task {

        private final String title;
        private final String description;

        public Task(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }
}
