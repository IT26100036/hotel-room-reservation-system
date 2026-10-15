<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Bookings list - /booking/list
  From BookingController.list():
    bookings        list of Booking to show
    selectedStatus  chosen filter (empty = All)
    statuses        all BookingStatus values, for the filter buttons
    success, error  one-time messages after an action
--%>
<jsp:include page="/WEB-INF/jsp/common/header.jsp">
    <jsp:param name="title"  value="Bookings"/>
    <jsp:param name="active" value="bookings"/>
</jsp:include>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2>Bookings</h2>
        <p>Walk-in and reservation bookings, newest first.</p>
    </div>
    <a class="btn btn-navy btn-sm" href="${ctx}/booking/new">+ New walk-in</a>
</div>

<%-- Messages from the last action --%>
<c:if test="${not empty success}">
    <div class="alert alert-success py-2">${success}</div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger py-2">${error}</div>
</c:if>

<%-- Status filter: the chosen one is filled, the others are outlined --%>
<div class="d-flex flex-wrap gap-2 mb-3">
    <a class="btn btn-sm ${empty selectedStatus ? 'btn-navy' : 'btn-outline-secondary'}"
       href="${ctx}/booking/list">All</a>
    <c:forEach var="s" items="${statuses}">
        <a class="btn btn-sm ${selectedStatus == s ? 'btn-navy' : 'btn-outline-secondary'}"
           href="${ctx}/booking/list?status=${s}">${s}</a>
    </c:forEach>
</div>

<div class="panel">
    <table class="table table-hover align-middle">
        <thead>
        <tr>
            <th>Booking</th>
            <th>Guest</th>
            <th>Room</th>
            <th>Check-in</th>
            <th>Check-out</th>
            <th>Nights</th>
            <th>Type</th>
            <th>Status</th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="b" items="${bookings}">
            <tr>
                <td class="mono">${b.displayName}</td>
                <td>${b.guestName}</td>
                <td class="mono">${b.roomNo}</td>
                <td class="mono">${b.dateFrom}</td>
                <td class="mono">${b.dateTo}</td>
                <td>${b.nights}</td>
                <td>${b.bookingType == 'WALK_IN' ? 'Walk-in' : 'Reservation'}</td>
                <td>
                    <%-- One badge colour per status --%>
                    <c:choose>
                        <c:when test="${b.status == 'CONFIRMED'}"><span class="badge bg-primary">Confirmed</span></c:when>
                        <c:when test="${b.status == 'CHECKED_IN'}"><span class="badge bg-success">Checked in</span></c:when>
                        <c:when test="${b.status == 'CHECKED_OUT'}"><span class="badge bg-secondary">Checked out</span></c:when>
                        <c:otherwise><span class="badge bg-danger">Cancelled</span></c:otherwise>
                    </c:choose>
                </td>
                <td class="text-end">
                    <a class="btn btn-sm btn-outline-secondary" href="${ctx}/booking/${b.id}">View</a>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty bookings}">
            <tr>
                <td colspan="9" class="text-center text-muted py-4">No bookings found</td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
