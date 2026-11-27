package com.bloodconnect.servlet;

import com.bloodconnect.service.DonorEligibilityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Serves the public donor eligibility checker at the exact URL /eligibility.
 *
 * <p>The exact mapping is deliberate: a prefix such as /checker/* would also catch
 * forwards and could send the request back into this servlet.</p>
 */
@WebServlet(name = "EligibilityServlet", urlPatterns = "/eligibility")
public class EligibilityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW_PATH = "/WEB-INF/views/eligibility.jsp";

    private final DonorEligibilityService eligibilityService = new DonorEligibilityService();

    /**
     * Shows the empty checker form.
     *
     * @param request  the current request
     * @param response the current response
     * @throws ServletException if the container cannot forward
     * @throws IOException      if the response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW_PATH).forward(request, response);
    }

    /**
     * Validates the submitted date and forwards the result back to the JSP.
     *
     * @param request  the current request
     * @param response the current response
     * @throws ServletException if the container cannot forward
     * @throws IOException      if the response cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String rawDate = request.getParameter("lastDonationDate");
        boolean available = request.getParameter("available") != null;
        request.setAttribute("lastDonationDate", rawDate == null ? "" : rawDate);
        request.setAttribute("available", available);

        try {
            LocalDate lastDonationDate = rawDate == null || rawDate.isBlank()
                    ? null : LocalDate.parse(rawDate.trim());
            DonorEligibilityService.EligibilityResult result =
                    eligibilityService.evaluate(available, lastDonationDate, LocalDate.now());
            request.setAttribute("result", result);
        } catch (DateTimeParseException e) {
            request.setAttribute("errorMessage", "Enter a valid date in YYYY-MM-DD format.");
        }

        request.getRequestDispatcher(VIEW_PATH).forward(request, response);
    }
}
