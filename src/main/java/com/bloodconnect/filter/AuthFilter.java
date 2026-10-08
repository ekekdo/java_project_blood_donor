package com.bloodconnect.filter;

import com.bloodconnect.enums.ProtectedArea;
import com.bloodconnect.enums.Role;
import com.bloodconnect.util.SessionUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Blocks every /admin/*, /donor/*, /patient/* and /hospital/* URL unless the
 * session holds a signed-in user with the matching role.
 *
 * WHY A FILTER AND NOT A CHECK INSIDE EACH SERVLET:
 * 1. It runs BEFORE the servlet, so a protected page is never rendered even briefly.
 * 2. Write it once. Four checks duplicated in four servlets is four chances to
 * forget one, and a forgotten check is a security hole.
 * 3. It is the textbook answer to "how do you protect pages in a Servlet app".
 *
 * WHY NOT A web.xml <security-constraint>: that would need role names as
 * <role-name> strings with no link to our Role enum, and it cannot show a friendly
 * "please sign in" page. A Filter lets us use the enum directly.
 */
@WebFilter(urlPatterns = {
        "/admin/*",
        "/donor/*",
        "/patient/*",
        "/hospital/*"
})
public class AuthFilter implements Filter {

    /**
     * The core of the filter: first path segment -> the one role allowed in.
     *
     * WHY a plain unmodifiable Map and NOT an EnumMap here (a question worth
     * getting right in the viva): EnumMap is fast because it indexes by the enum's
     * ORDINAL, which only works when the KEY is an enum. These keys are URL path
     * segments - Strings - so EnumMap is not even applicable. The enum-based
     * alternative is ProtectedArea, which is what actually carries the
     * String -> Role knowledge; see PROTECTED_AREAS_BY_PATH below.
     *
     * WHY it is a STATIC final constant: it is built once when the class loads, not
     * on every request. A filter instance is shared by all request threads, so
     * per-request rebuilding would also be wasted work.
     */
    private static final Map<String, ProtectedArea> PROTECTED_AREAS_BY_PATH = buildProtectedAreaIndex();

    private static Map<String, ProtectedArea> buildProtectedAreaIndex() {
        Map<String, ProtectedArea> index = new HashMap<>();
        for (ProtectedArea area : ProtectedArea.values()) {
            index.put(area.getPathSegment(), area);
        }
        // Unmodifiable so no request thread can ever add or remove an area. Without
        // this, a bug elsewhere could silently open /admin to everyone.
        return Collections.unmodifiableMap(index);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String pathWithinApp = httpRequest.getServletPath();
        if (pathWithinApp == null) {
            pathWithinApp = "";
        }

        // getServletPath() is "/donor/dashboard"; the area name is the segment after
        // the leading slash.
        String area = extractArea(pathWithinApp);
        ProtectedArea protectedArea = PROTECTED_AREAS_BY_PATH.get(area);
        Role requiredRole = protectedArea == null ? null : protectedArea.getRequiredRole();

        // Not a protected area (should not happen given the urlPatterns, but a
        // default of "let it through" is the safe behaviour for an unknown path).
        if (requiredRole == null) {
            chain.doFilter(request, response);
            return;
        }

        // STEP 1: is anyone signed in at all?
        if (!SessionUtil.isLoggedIn(httpRequest)) {
            sendToLogin(httpRequest, httpResponse, "Please sign in to open your dashboard.");
            return;
        }

        // STEP 2: is it the RIGHT person?
        Role loggedInRole = SessionUtil.getLoggedInRole(httpRequest);
        if (loggedInRole != requiredRole) {
            // WHY 403 and not a redirect: the visitor IS authenticated, so sending
            // them to the login page would be confusing. 403 Forbidden is the honest
            // answer - they are logged in but not allowed here.
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "You are signed in as a " + loggedInRole + " account. "
                            + "This page is only available to " + requiredRole + " accounts.");
            return;
        }

        // STEP 3: authorised. Let the request through.
        chain.doFilter(request, response);
    }

    /**
     * Redirects a logged-out visitor to the login page.
     *
     * WHY the request URI is remembered in the session: after signing in, the user
     * lands on the page they originally wanted instead of the generic dashboard.
     * That is a small courtesy, but it is the kind of detail worth mentioning.
     *
     * WHY 302 (sendRedirect) and not a forward: a forward would show /donor/dashboard
     * in the address bar while showing the login page, which is confusing and can
     * be cached.
     */
    private void sendToLogin(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {

        // Only remember GET targets. Remembering a POST target and then replaying it
        // as a GET after login is a well-known way to break a form.
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            SessionUtil.setFlash(request, message);
        }
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    /**
     * "/donor/dashboard" -> "donor".
     *
     * Why not request.getRequestURI(): that includes the context path
     * ("/blood-connect/donor/dashboard"), so the same filter would break the moment
     * the app is deployed under a different context name. getServletPath() is always
     * relative to the context.
     */
    private String extractArea(String servletPath) {
        if (servletPath.isEmpty() || servletPath.equals("/")) {
            return "";
        }
        String withoutLeadingSlash = servletPath.startsWith("/") ? servletPath.substring(1) : servletPath;
        int slashIndex = withoutLeadingSlash.indexOf('/');
        return slashIndex == -1
                ? withoutLeadingSlash
                : withoutLeadingSlash.substring(0, slashIndex);
    }
}
