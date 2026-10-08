<%--
    index.jsp - the public landing page.

    WHY the copy is specific and there are no made-up statistics:
    every number a blood-donation site shows is a real figure from a real
    registry. Inventing "12,450 donors registered" would be dishonest, and it
    would also be trivially wrong the moment somebody opened the database. So
    this page states what the system actually does today and links to the parts
    that are built. Anything not built yet is labelled as such.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Home"/>

<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container py-5">

    <div class="row align-items-center g-5">
        <div class="col-lg-7">
            <span class="badge text-bg-light border mb-3">University project &middot; Phase 1</span>
            <h1 class="display-5 fw-bold">
                Coordinating blood donors during an emergency
            </h1>
            <p class="lead text-secondary">
                A web application where registered donors, hospitals and patients share one
                place to raise and answer blood requests &mdash; instead of phone calls and
                group chats that nobody can search afterwards.
            </p>
            <p class="text-secondary">
                This build currently covers <strong>accounts and access control</strong>:
                registration with role-specific fields, BCrypt-hashed passwords, session-based
                sign-in, and a filter that keeps each role inside its own area.
            </p>

            <div class="d-flex gap-2 flex-wrap mt-4">
                <a class="btn btn-danger px-4" href="${pageContext.request.contextPath}/register.jsp">
                    Create an account
                </a>
                <a class="btn btn-outline-secondary px-4" href="${pageContext.request.contextPath}/login.jsp">
                    Sign in
                </a>
            </div>

            <c:choose>
                <c:when test="${param.loggedOut eq '1'}">
                    <div class="alert alert-success mt-4 mb-0" role="alert">
                        You have been signed out and your session was destroyed.
                    </div>
                </c:when>
            </c:choose>
        </div>

        <div class="col-lg-5">
            <div class="card shadow-sm border-0">
                <div class="card-body p-4">
                    <h2 class="h5 card-title mb-3">How the matching will work</h2>
                    <ol class="small text-secondary mb-0 ps-3">
                        <li class="mb-2">
                            A verified hospital or a patient raises a request for a blood group
                            and a city.
                        </li>
                        <li class="mb-2">
                            The system finds donors who are
                            <strong>available</strong> and whose last donation was
                            <strong>90 or more days ago</strong>, and whose group is
                            compatible.
                        </li>
                        <li class="mb-2">
                            Those donors are notified and accept, decline or say maybe.
                        </li>
                        <li>
                            When the units are collected, the donation, the request status and
                            the donor&rsquo;s next eligible date are written in a
                            <strong>single database transaction</strong>.
                        </li>
                    </ol>
                </div>
            </div>
        </div>
    </div>

    <hr class="my-5">

    <h2 class="h4 mb-1">Who uses it</h2>
    <p class="text-secondary small mb-4">
        The four account types below are not cosmetic. Each one is a subclass of
        <code>User</code> that decides its own dashboard, and a servlet filter refuses
        every area to the wrong role.
    </p>

    <div class="row g-3">
        <div class="col-md-6 col-lg-3">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body">
                    <h3 class="h6">Donor</h3>
                    <p class="small text-secondary mb-0">
                        Records age, gender, blood group and city. Eligibility is computed
                        from the last donation date, not typed in by hand.
                    </p>
                </div>
            </div>
        </div>
        <div class="col-md-6 col-lg-3">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body">
                    <h3 class="h6">Hospital</h3>
                    <p class="small text-secondary mb-0">
                        Records registration number and authorised staff member. Starts
                        unverified, and an unverified hospital cannot raise requests.
                    </p>
                </div>
            </div>
        </div>
        <div class="col-md-6 col-lg-3">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body">
                    <h3 class="h6">Patient</h3>
                    <p class="small text-secondary mb-0">
                        Records city and an emergency contact, then raises a request for
                        itself or a relative.
                    </p>
                </div>
            </div>
        </div>
        <div class="col-md-6 col-lg-3">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body">
                    <h3 class="h6">Admin</h3>
                    <p class="small text-secondary mb-0">
                        Verifies hospitals and manages accounts. Cannot be created from the
                        registration page; it exists only in the seed data.
                    </p>
                </div>
            </div>
        </div>
    </div>

    <hr class="my-5" id="compatibility">

    <div class="row g-4 align-items-start">
        <div class="col-lg-4">
            <h2 class="h4 mb-2">Blood group compatibility</h2>
            <p class="text-secondary small">
                Transfusion compatibility is fixed biology, so this table is built once when
                the application starts and is then made read-only. It is not something a user
                can change, which is why it belongs in code rather than in the database.
            </p>
            <p class="text-secondary small mb-0">
                <strong>O&minus;</strong> can donate to every group.
                <strong>AB+</strong> can receive from every group. Rh&minus; can only receive
                Rh&minus;.
            </p>
        </div>
        <div class="col-lg-8">
            <div class="table-responsive">
                <table class="table table-sm table-bordered bg-white align-middle">
                    <caption class="caption-top small text-secondary">
                        Which donor groups a patient of each group can receive
                    </caption>
                    <thead class="table-light">
                        <tr>
                            <th scope="col">Patient group</th>
                            <th scope="col">Can receive blood from</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><th scope="row">O&minus;</th><td class="text-secondary">O&minus; only</td></tr>
                        <tr><th scope="row">O+</th><td class="text-secondary">O&minus;, O+</td></tr>
                        <tr><th scope="row">A&minus;</th><td class="text-secondary">O&minus;, A&minus;</td></tr>
                        <tr><th scope="row">A+</th><td class="text-secondary">O&minus;, O+, A&minus;, A+</td></tr>
                        <tr><th scope="row">B&minus;</th><td class="text-secondary">O&minus;, B&minus;</td></tr>
                        <tr><th scope="row">B+</th><td class="text-secondary">O&minus;, O+, B&minus;, B+</td></tr>
                        <tr><th scope="row">AB&minus;</th><td class="text-secondary">O&minus;, A&minus;, B&minus;, AB&minus;</td></tr>
                        <tr><th scope="row">AB+</th><td class="text-secondary">Every group &mdash; the universal recipient</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <hr class="my-5">

    <div class="card border-0 shadow-sm">
        <div class="card-body p-4">
            <h2 class="h5 mb-3">What is not built yet</h2>
            <p class="small text-secondary mb-3">
                Being explicit about this is deliberate. These are the next phases of the
                project, and the schema and interfaces for them already exist so they can be
                added without redesigning anything:
            </p>
            <ul class="small text-secondary mb-0">
                <li>Raising blood requests and browsing nearby requests.</li>
                <li>Notifying eligible donors through a background worker thread.</li>
                <li>The single-transaction fulfilment of a request.</li>
                <li>Admin screens for verifying hospitals and reading the audit log.</li>
            </ul>
        </div>
    </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
