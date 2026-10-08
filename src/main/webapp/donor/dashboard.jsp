<%--
    /donor/dashboard.jsp - reached via the URL /donor/dashboard.

    WHY the URL does not end in ".jsp": AuthFilter guards /donor/*, so the user sees
    a clean /donor/dashboard. DashboardServlet receives that URL and forwards to this
    file. request.getRequestDispatcher().forward() keeps the address bar unchanged,
    which is why the filter matched the right area.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Donor dashboard"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<%@ include file="/WEB-INF/views/dashboard-common.jspf" %>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
