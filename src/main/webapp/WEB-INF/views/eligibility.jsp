<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Donor eligibility checker"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <h1 class="h3 mb-2">Donor eligibility checker</h1>
                    <p class="text-secondary">
                        Enter the last donation date to see whether the 90-day rest period has ended.
                        This checker runs entirely in Java and does not store any entered data.
                    </p>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-warning" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <form method="post" action="${pageContext.request.contextPath}/eligibility">
                        <div class="mb-3">
                            <label class="form-label" for="lastDonationDate">Last donation date</label>
                            <input class="form-control" type="date" id="lastDonationDate"
                                   name="lastDonationDate" value="${lastDonationDate}">
                            <div class="form-text">Leave empty if the donor has never donated.</div>
                        </div>
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" id="available"
                                   name="available" <c:if test="${available}">checked</c:if>>
                            <label class="form-check-label" for="available">Donor is currently available</label>
                        </div>
                        <button class="btn btn-danger" type="submit">Check eligibility</button>
                    </form>

                    <c:if test="${not empty result}">
                        <hr class="my-4">
                        <div class="alert <c:out value='${result.eligible ? "alert-success" : "alert-secondary"}'/>"
                             role="status">
                            <strong><c:out value="${result.eligible ? 'Eligible' : 'Not eligible yet'}"/></strong>
                            <div><c:out value="${result.message}"/></div>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
