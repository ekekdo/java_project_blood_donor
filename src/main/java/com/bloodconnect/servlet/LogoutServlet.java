package com.bloodconnect.servlet;

import com.bloodconnect.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles /logout.
 *
 * WHY BOTH doGet AND doPost ARE ALLOWED HERE:
 * A logout changes no server state that an attacker could abuse - at worst someone
 * logs you out. Idempotent, harmless, and it lets a plain <a href> link work, which
 * is what the header uses. The register/login servlets are different: those DO
 * change state, so they are POST-only.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogout(request, response);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // invalidate() destroys the whole session: every attribute goes, and the
        // session id the browser holds becomes invalid. That is strictly stronger
        // than removing our two keys, which would leave a live but empty session and
        // a still-valid cookie for an attacker to reuse.
        SessionUtil.destroySession(request);

        // WHY an extra cookie clear: some browsers keep a copy of the session cookie
        // (e.g. after a crash), so removing it client-side too makes the logout
        // visually obvious and stops the stale cookie being presented again.
        var cookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath());
        response.addCookie(cookie);

        response.sendRedirect(request.getContextPath() + "/index.jsp?loggedOut=1");
    }
}
