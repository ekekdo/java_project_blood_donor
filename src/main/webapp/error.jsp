<%--
    error.jsp - the container's error page for 403, 404, 500 and any uncaught
    Throwable, as declared in web.xml.

    WHY it is a JSP and not Tomcat's default page: the default is unstyled raw
    HTML, so a user who trips the 403 in AuthFilter would land somewhere that
    looks like a different website entirely.

    WHY it must not leak the exception message:
      request.getAttribute("jakarta.servlet.error.message") on a 500 contains the
      exception's getMessage(), and the default Tomcat message can contain a stack
      trace fragment or part of a SQL statement. We show a plain explanation and let
      the server log hold the detail.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
    These four attributes are the standard names the servlet container sets when it
    forwards to an error page. They are request attributes, and this page is reached
    by a FORWARD from the container, so unlike a redirect they survive.
--%>
<c:set var="statusCode" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="exceptionType" value="${requestScope['jakarta.servlet.error.exception_type']}"/>
<c:set var="requestUri" value="${requestScope['jakarta.servlet.error.request_uri']}"/>

<c:choose>
    <c:when test="${statusCode == 403}">
        <c:set var="pageTitle" value="Not allowed"/>
        <c:set var="heading" value="This page is not available to your account type"/>
        <c:set var="explanation"
               value="You are signed in, but this area belongs to a different role. If you think that is wrong, sign out and sign back in, or ask an administrator to check your account."/>
    </c:when>
    <c:when test="${statusCode == 404}">
        <c:set var="pageTitle" value="Page not found"/>
        <c:set var="heading" value="We could not find that page"/>
        <c:set var="explanation"
               value="The address may be mistyped, or the page may have been moved. The links in the navigation bar should get you back."/>
    </c:when>
    <c:when test="${statusCode == 500}">
        <c:set var="pageTitle" value="Server error"/>
        <c:set var="heading" value="Something went wrong on our side"/>
        <c:set var="explanation"
               value="The request could not be completed. The details have been written to the server log. Please try again, and if it keeps happening contact the administrator."/>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="Error"/>
        <c:set var="heading" value="Something went wrong"/>
        <c:set var="explanation"
               value="The request could not be completed. The details have been written to the server log."/>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card shadow-sm border-0">
                <div class="card-body p-4 p-md-5 text-center">
                    <p class="display-4 fw-bold text-danger mb-2">
                        <c:out value="${statusCode}" default="Error"/>
                    </p>
                    <h1 class="h4 mb-3"><c:out value="${heading}"/></h1>
                    <p class="text-secondary"><c:out value="${explanation}"/></p>

                    <div class="d-flex gap-2 justify-content-center flex-wrap mt-4">
                        <a class="btn btn-danger" href="${pageContext.request.contextPath}/index.jsp">
                            Go to the home page
                        </a>
                        <c:if test="${not empty sessionScope.bloodConnectUserId}">
                            <a class="btn btn-outline-secondary"
                               href="${pageContext.request.contextPath}${sessionScope.bloodConnectDashboardPath}">
                                My dashboard
                            </a>
                        </c:if>
                    </div>

                    <%--
                        The technical detail is shown in a collapsed block and the
                        exception class name is the only thing shown, never the message.
                        It helps while developing without telling an attacker about the
                        schema or the SQL that failed.
                    --%>
                    <c:if test="${not empty statusCode}">
                        <hr class="my-4">
                        <details class="text-start">
                            <summary class="small text-secondary" style="cursor:pointer">
                                Technical details
                            </summary>
                            <dl class="row small text-secondary mt-2 mb-0">
                                <dt class="col-sm-4">Status code</dt>
                                <dd class="col-sm-8"><c:out value="${statusCode}"/></dd>

                                <c:if test="${not empty requestUri}">
                                    <dt class="col-sm-4">Requested</dt>
                                    <dd class="col-sm-8"><code><c:out value="${requestUri}"/></code></dd>
                                </c:if>

                                <c:if test="${not empty exceptionType}">
                                    <dt class="col-sm-4">Exception type</dt>
                                    <dd class="col-sm-8"><code><c:out value="${exceptionType}"/></code></dd>
                                </c:if>
                            </dl>
                        </details>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
