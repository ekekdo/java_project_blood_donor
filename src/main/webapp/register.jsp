<%--
    register.jsp - one form for three account types.

    WHY ONE PAGE WITH A RADIO GROUP INSTEAD OF THREE PAGES:
      * "Which fields are required" is a genuine business rule, so showing it in one
        place makes the rule visible instead of hiding it in three separate forms.
      * RegisterServlet already reads every field and ignores the ones that do not
        belong to the chosen role, so the server is the single authority either way.
      * Fewer pages means the validation messages from AuthService can be shown next to
        the field they belong to, which ValidationException.getField() supports.

    WHY THE ROLE LIST HAS THREE ENTRIES AND NOT FOUR:
      ADMIN is deliberately absent. AuthService.register() also rejects role=ADMIN, so
      the restriction does not depend on the radio buttons being honest - a crafted
      POST cannot create an administrator.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
    requestScope.form is the RegisterForm bean RegisterServlet put back on the
    request when validation failed, so the page can be re-rendered with the
    values the user already typed. That is why nothing has to be re-entered
    after a single mistake.
--%>
<c:set var="form" value="${requestScope.form}"/>
<c:set var="errorField" value="${requestScope.errorField}"/>
<c:set var="pageTitle" value="Register"/>

<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-8">

            <h1 class="h3 mb-1">Create an account</h1>
            <p class="text-secondary small mb-4">
                Choose the account type that matches what you are here to do. The extra
                fields appear as soon as you select a type.
            </p>

            <c:if test="${not empty requestScope.errorMessage}">
                <div class="alert alert-danger" role="alert">
                    <c:out value="${requestScope.errorMessage}"/>
                </div>
            </c:if>

            <div class="card card-form shadow-sm border-0">
                <div class="card-body p-4">

                    <form method="post" action="${pageContext.request.contextPath}/register" novalidate>

                        <%-- ================= ACCOUNT TYPE ================= --%>
                        <fieldset class="mb-4">
                            <legend class="h6">I am registering as</legend>

                            <%--
                                Radio buttons rather than a <select>: all three options
                                should be visible at once, because the user has to read
                                them to choose, and a dropdown hides two of the three
                                behind a click.
                            --%>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="roleDonor"
                                       value="DONOR"
                                       <c:if test="${form.role eq 'DONOR' or (empty form.role and param.role eq 'DONOR')}">checked</c:if>>
                                <label class="form-check-label" for="roleDonor">
                                    <strong>Donor</strong>
                                    <span class="d-block small text-secondary">
                                        I want to donate blood. Requires age, gender, blood group and city.
                                    </span>
                                </label>
                            </div>

                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="roleHospital"
                                       value="HOSPITAL"
                                       <c:if test="${form.role eq 'HOSPITAL' or (empty form.role and param.role eq 'HOSPITAL')}">checked</c:if>>
                                <label class="form-check-label" for="roleHospital">
                                    <strong>Hospital / blood bank</strong>
                                    <span class="d-block small text-secondary">
                                        I want to raise blood requests. Requires hospital name,
                                        registration number and an authorised contact. Accounts are
                                        verified by an administrator before requests can be raised.
                                    </span>
                                </label>
                            </div>

                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="rolePatient"
                                       value="PATIENT"
                                       <c:if test="${form.role eq 'PATIENT' or (empty form.role and param.role eq 'PATIENT')}">checked</c:if>>
                                <label class="form-check-label" for="rolePatient">
                                    <strong>Patient</strong>
                                    <span class="d-block small text-secondary">
                                        I need blood for myself or a relative. Requires a city.
                                    </span>
                                </label>
                            </div>
                        </fieldset>

                        <hr>

                        <%-- ================= SHARED FIELDS ================= --%>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label for="name" class="form-label label-required">Full name</label>
                                <input type="text" class="form-control" id="name" name="name"
                                       value="<c:out value='${form.name}'/>"
                                       required maxlength="100" autocomplete="name">
                                <%-- c:if on errorField puts the server's message under the
                                     exact input it belongs to, not in a generic banner. --%>
                                <c:if test="${errorField eq 'name'}">
                                    <div class="small text-danger mt-1">Please check this field.</div>
                                </c:if>
                            </div>

                            <div class="col-md-6">
                                <label for="email" class="form-label label-required">Email address</label>
                                <input type="email" class="form-control" id="email" name="email"
                                       value="<c:out value='${form.email}'/>"
                                       required maxlength="255" autocomplete="email">
                            </div>

                            <div class="col-md-6">
                                <label for="phone" class="form-label label-required">Mobile number</label>
                                <input type="tel" class="form-control" id="phone" name="phone"
                                       value="<c:out value='${form.phone}'/>"
                                       required maxlength="15" placeholder="98XXXXXXXX"
                                       autocomplete="tel">
                                <div class="form-text">10 digits, starting with 6&ndash;9.</div>
                            </div>

                            <div class="col-md-6">
                                <label for="password" class="form-label label-required">Password</label>
                                <input type="password" class="form-control" id="password" name="password"
                                       required autocomplete="new-password">
                                <%--
                                    The rules are stated up front rather than discovered by
                                    failing. AuthService enforces exactly these, and the
                                    error comes back to this page if they are missed.
                                --%>
                                <div class="form-text">
                                    At least 8 characters, with at least one letter and one number.
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label for="city" class="form-label label-required">City</label>
                                <input type="text" class="form-control" id="city" name="city"
                                       value="<c:out value='${form.city}'/>"
                                       required maxlength="60" autocomplete="address-level2">
                                <div class="form-text">Used to match you with nearby requests.</div>
                            </div>
                        </div>

                        <%--
                            The two blocks below carry the class role-only-section, which
                            app.css hides. The small script at the bottom of this page
                            shows the right one when a radio is chosen.
                        --%>

                        <%-- ================= DONOR-ONLY FIELDS ================= --%>
                        <div class="role-only-section mt-4" id="donorSection">
                            <h2 class="h6 border-top pt-3">Donor details</h2>
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label for="age" class="form-label label-required">Age</label>
                                    <input type="number" class="form-control" id="age" name="age"
                                           value="<c:out value='${form.age}'/>"
                                           min="18" max="65" required>
                                    <div class="form-text">18&ndash;65.</div>
                                </div>

                                <div class="col-md-4">
                                    <label for="gender" class="form-label label-required">Gender</label>
                                    <select class="form-select" id="gender" name="gender" required>
                                        <option value="">Select&hellip;</option>
                                        <%-- A closed list, matching the check in AuthService.buildDonor(). --%>
                                        <option value="Male"   <c:if test="${form.gender eq 'Male'}">selected</c:if>>Male</option>
                                        <option value="Female" <c:if test="${form.gender eq 'Female'}">selected</c:if>>Female</option>
                                        <option value="Other"  <c:if test="${form.gender eq 'Other'}">selected</c:if>>Other</option>
                                    </select>
                                </div>

                                <div class="col-md-4">
                                    <label for="bloodGroup" class="form-label label-required">Blood group</label>
                                    <select class="form-select" id="bloodGroup" name="bloodGroup" required>
                                        <option value="">Select&hellip;</option>
                                        <%--
                                            The options are NOT hard-coded. AppContextListener
                                            publishes BloodGroup.values() into application
                                            scope when the application starts, so the enum stays
                                            the single source of truth and adding a group
                                            automatically adds it to this form.
                                        --%>
                                        <c:forEach var="group" items="${applicationScope.bloodGroups}">
                                            <option value="${group.label}"
                                                    <c:if test="${form.bloodGroup eq group.label}">selected</c:if>>
                                                <c:out value="${group.label}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div class="col-12">
                                    <label for="donorAddress" class="form-label">Address</label>
                                    <input type="text" class="form-control" id="donorAddress" name="address"
                                           value="<c:out value='${form.address}'/>"
                                           maxlength="255" autocomplete="street-address">
                                </div>
                            </div>
                        </div>

                        <%-- ================= HOSPITAL-ONLY FIELDS ================= --%>
                        <div class="role-only-section mt-4" id="hospitalSection">
                            <h2 class="h6 border-top pt-3">Hospital details</h2>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label for="hospitalName" class="form-label label-required">Hospital / bank name</label>
                                    <input type="text" class="form-control" id="hospitalName" name="hospitalName"
                                           value="<c:out value='${form.hospitalName}'/>" required maxlength="150">
                                </div>

                                <div class="col-md-6">
                                    <label for="registrationNo" class="form-label label-required">Registration number</label>
                                    <input type="text" class="form-control" id="registrationNo" name="registrationNo"
                                           value="<c:out value='${form.registrationNo}'/>" required maxlength="50">
                                    <div class="form-text">Must be unique across all registered hospitals.</div>
                                </div>

                                <div class="col-12">
                                    <label for="hospitalAddress" class="form-label label-required">Hospital address</label>
                                    <input type="text" class="form-control" id="hospitalAddress" name="address"
                                           value="<c:out value='${form.address}'/>" required maxlength="255">
                                </div>

                                <div class="col-md-6">
                                    <label for="hospitalPhone" class="form-label label-required">Hospital contact number</label>
                                    <input type="tel" class="form-control" id="hospitalPhone" name="hospitalPhone"
                                           value="<c:out value='${form.hospitalPhone}'/>"
                                           required maxlength="15">
                                </div>

                                <div class="col-md-6">
                                    <label for="authorizedPerson" class="form-label label-required">Authorised person</label>
                                    <input type="text" class="form-control" id="authorizedPerson" name="authorizedPerson"
                                           value="<c:out value='${form.authorizedPerson}'/>" required maxlength="100">
                                    <div class="form-text">The staff member accountable for raising requests.</div>
                                </div>
                            </div>
                        </div>

                        <%-- ================= PATIENT-ONLY FIELDS ================= --%>
                        <div class="role-only-section mt-4" id="patientSection">
                            <h2 class="h6 border-top pt-3">Patient details</h2>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label for="emergencyContact" class="form-label">Emergency contact</label>
                                    <input type="tel" class="form-control" id="emergencyContact"
                                           name="emergencyContact"
                                           value="<c:out value='${form.emergencyContact}'/>" maxlength="15">
                                    <div class="form-text">Optional. Used if you cannot be reached directly.</div>
                                </div>
                            </div>
                        </div>

                        <div class="alert alert-light border small mt-4 mb-3" role="note">
                            Your password is stored as a salted BCrypt hash. The plain text is
                            never written to the database, and nobody including the
                            administrator can read it back.
                        </div>

                        <button type="submit" class="btn btn-danger px-4">Create account</button>
                        <a class="btn btn-link px-3" href="${pageContext.request.contextPath}/login.jsp">
                            I already have an account
                        </a>
                    </form>
                </div>
            </div>

        </div>
    </div>
</div>

<%--
    A short, dependency-free script for the field toggling.

    WHY inline JavaScript instead of a separate .js file for twelve lines:
    it is genuinely small, and keeping it next to the markup it controls means the
    ids cannot drift out of sync. (The navbar dropdown still uses Bootstrap's own
    bundle, so this is the ONLY hand-written script in the project.)

    WHY "add is-visible" instead of setting style.display directly:
    the visible/hidden state is described in app.css as a class, so the behaviour
    lives in one stylesheet rather than being split between CSS and JS.
--%>
<script>
    (function () {
        var roleInputs = document.querySelectorAll('input[name="role"]');
        var sections = {
            DONOR:    document.getElementById('donorSection'),
            HOSPITAL: document.getElementById('hospitalSection'),
            PATIENT:  document.getElementById('patientSection')
        };

        function refresh() {
            // The radio group allows at most one selection, so one loop over all
            // sections is enough: show the chosen one, hide the rest.
            Object.keys(sections).forEach(function (role) {
                var selected = document.querySelector('input[name="role"][value="' + role + '"]');
                if (selected && selected.checked) {
                    sections[role].classList.add('is-visible');
                } else {
                    sections[role].classList.remove('is-visible');
                }
            });
        }

        roleInputs.forEach(function (input) {
            input.addEventListener('change', refresh);
        });

        // Run once on load: when the page is re-rendered after a validation
        // failure, the previously chosen role is already checked, so its fields
        // must be visible immediately instead of staying hidden.
        refresh();
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
