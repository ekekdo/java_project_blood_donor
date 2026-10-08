<%--
    login.jsp - email + password only.

    WHY no "confirm password" or "remember me" here:
      * confirm password matters when you are CHOOSING a password, so it belongs on
        register.jsp, not here.
      * "remember me" would need a second, long-lived token and a second table.
        Adding a half-working version would be worse than leaving it out.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign in"/>

<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-7 col-lg-5">

            <h1 class="h3 mb-1">Sign in</h1>
            <p class="text-secondary small mb-4">
                Use the email address you registered with.
            </p>

            <%-- Shown when AuthService.register() sent a hospital back here. --%>
            <c:if test="${param.registered eq '1'}">
                <div class="alert alert-success" role="alert">
                    Your hospital account was created. An administrator has to verify it
                    before you can sign in.
                </div>
            </c:if>

            <c:if test="${param.error eq 'alreadyLoggedIn'}">
                <div class="alert alert-info" role="alert">
                    You are already signed in.
                </div>
            </c:if>

            <%--
                The failure message set by LoginServlet before its redirect. It is
                deliberately the SAME text for "no such email" and "wrong password" so
                the page cannot be used to discover which emails have accounts.
            --%>
            <c:if test="${not empty requestScope.errorMessage}">
                <div class="alert alert-danger" role="alert">
                    <c:out value="${requestScope.errorMessage}"/>
                </div>
            </c:if>

            <div class="card card-form shadow-sm border-0">
                <div class="card-body p-4">
                    <%--
                        method="post" is not decoration. LoginServlet only accepts POST,
                        so a GET would not authenticate anyone. It also keeps the
                        password out of the address bar, the browser history and any
                        proxy log.
                    --%>
                    <form method="post" action="${pageContext.request.contextPath}/login" novalidate>

                        <div class="mb-3">
                            <label for="email" class="form-label label-required">Email address</label>
                            <input type="email" class="form-control" id="email" name="email"
                                   value="<c:out value='${requestScope.enteredEmail}'/>"
                                   required autocomplete="email" maxlength="255">
                        </div>

                        <div class="mb-4">
                            <label for="password" class="form-label label-required">Password</label>
                            <%--
                                type="password" hides the characters, and autocomplete
                                lets a password manager fill it in. There is deliberately
                                no "show password" toggle: this is a clinical system and
                                shoulder-surfing is a real risk.
                            --%>
                            <input type="password" class="form-control" id="password" name="password"
                                   required autocomplete="current-password">
                        </div>

                        <button type="submit" class="btn btn-danger w-100">Sign in</button>
                    </form>
                </div>
            </div>

            <p class="text-center small text-secondary mt-4 mb-0">
                No account yet?
                <a href="${pageContext.request.contextPath}/register.jsp">Register as a donor, hospital or patient</a>.
            </p>

            <div class="card border-0 shadow-sm mt-4">
                <div class="card-body p-3">
                    <h2 class="h6 mb-2">Test accounts from <code>db/sample_data.sql</code></h2>
                    <%--
                        Printing the seed credentials is safe HERE ONLY because this is a
                        local university demo with no real patient data. It is a
                        convenience for the examiner, and it is labelled with the file
                        the values come from so nobody mistakes them for real accounts.
                    --%>
                    <table class="table table-sm small mb-0">
                        <tbody>
                            <tr>
                                <th scope="row" class="fw-normal">Admin</th>
                                <td><code>admin@blood.com</code> / <code>Admin@123</code></td>
                            </tr>
                            <tr>
                                <th scope="row" class="fw-normal">Hospital (verified)</th>
                                <td><code>reception@citygeneral.in</code> / <code>Hospital@123</code></td>
                            </tr>
                            <tr>
                                <th scope="row" class="fw-normal">Hospital (unverified)</th>
                                <td><code>contact@sahyadri.org.in</code> &mdash; sign-in is refused on purpose</td>
                            </tr>
                            <tr>
                                <th scope="row" class="fw-normal">Donor</th>
                                <td><code>aarav@example.in</code> / <code>Donor@123</code></td>
                            </tr>
                            <tr>
                                <th scope="row" class="fw-normal">Patient</th>
                                <td><code>sanjay@example.in</code> / <code>Patient@123</code></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
