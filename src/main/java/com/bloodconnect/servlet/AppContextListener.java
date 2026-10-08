package com.bloodconnect.servlet;

import com.bloodconnect.enums.BloodGroup;
import com.bloodconnect.enums.Role;
import com.bloodconnect.enums.UrgencyLevel;
import com.bloodconnect.util.BloodCompatibility;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.EnumMap;
import java.util.Map;

/**
 * Publishes the read-only reference data that the JSP pages render.
 *
 * WHY THIS EXISTS: register.jsp has to draw a blood-group dropdown. Three options,
 * all bad:
 *   a) hard-code "O+, O-, A+..." in the JSP - the list is then duplicated between
 *      the view and the BloodGroup enum, and they can disagree;
 *   b) call BloodGroup.values() from EL - works, but puts Java logic in the view and
 *      needs a page import in every file;
 *   c) compute it ONCE here and share it - the view just iterates what is already
 *      there, the enum stays the single source of truth, and the work happens once
 *      per application start instead of once per request.
 *
 * WHY a ServletContextListener: it is the Servlet 3.0+ replacement for a
 * &lt;context-listener&gt; entry in web.xml, and it is invoked when the app starts
 * and stops. @WebListener means no web.xml edit.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    /** Request attribute name holding the blood groups, as a List. */
    public static final String ATTR_BLOOD_GROUPS = "bloodGroups";

    /** Request attribute name holding the roles offered on the registration form. */
    public static final String ATTR_ROLES = "bloodRoles";

    /** Request attribute name holding the compatibility table for the reference page. */
    public static final String ATTR_COMPATIBILITY = "bloodCompatibility";

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext context = event.getServletContext();

        // Arrays.asList() gives a fixed-size List, and List.of() an immutable one.
        // Either is enough here because nothing ever adds to it.
        context.setAttribute(ATTR_BLOOD_GROUPS, java.util.Arrays.asList(BloodGroup.values()));
        context.setAttribute(ATTR_ROLES, java.util.Arrays.asList(
                Role.DONOR, Role.HOSPITAL, Role.PATIENT));
        context.setAttribute(ATTR_COMPATIBILITY, BloodCompatibility.fullCompatibilityTable());

        context.log("BloodConnect started. Published "
                + BloodGroup.values().length + " blood groups and "
                + BloodCompatibility.fullCompatibilityTable().size()
                + " compatibility rules to application scope.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // WHY anything at all: Tomcat hot-redeploys without a clean JVM shutdown, so
        // an explicit log line is how you tell a redeploy from a crash.
        event.getServletContext().log("BloodConnect stopping. Releasing application-scope data.");
    }

    /**
     * Kept only to document the type the compatibility attribute holds.
     *
     * @return recipient group -&gt; set of donor groups that can help
     */
    public static Map<BloodGroup, java.util.Set<BloodGroup>> describeCompatibility() {
        return new EnumMap<>(BloodCompatibility.fullCompatibilityTable());
    }

    /**
     * The urgency levels, published for a future request form.
     * @return the levels ordered by their explicit priority
     */
    public static java.util.List<UrgencyLevel> urgencyLevelsByPriority() {
        return java.util.Arrays.stream(UrgencyLevel.values())
                .sorted(java.util.Comparator.comparingInt(UrgencyLevel::getPriority))
                .toList();
    }
}
