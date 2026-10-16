<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Bookings list - /booking/list
  From BookingController.list():
    bookings        list of Booking to show
    selectedStatus  chosen filter (empty = All)
    statuses        all BookingStatus values, for the filter chips
    counts          number of bookings per status, totalCount = all bookings
    success, error  one-time messages after an action
--%>
<jsp:include page="/WEB-INF/jsp/common/header.jsp">
    <jsp:param name="title"  value="Bookings"/>
    <jsp:param name="active" value="bookings"/>
</jsp:include>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<link rel="stylesheet" href="${ctx}/css/booking.css">

<div class="page-head">
    <div>
        <h2>Bookings</h2>
        <p>Confirmed stays from reservations and walk-ins, from check-in to check-out.</p>
    </div>
    <a class="btn btn-navy" href="${ctx}/booking/new">Walk-in booking</a>
</div>

<%-- Messages from the last action --%>
<c:if test="${not empty success}">
    <div class="alert alert-success py-2">${success}</div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger py-2">${error}</div>
</c:if>

<%-- Status filter with a count on each chip; the chosen chip is highlighted --%>
<div class="bk-filter">
    <a class="bk-chip bk-chip-all ${empty selectedStatus ? 'active' : ''}" href="${ctx}/booking/list">
        All<span class="count">${totalCount}</span>
    </a>
    <c:forEach var="s" items="${statuses}">
        <%-- tone = colour name used in booking.css, label = friendly text --%>
        <c:choose>
            <c:when test="${s == 'CONFIRMED'}"><c:set var="tone" value="confirmed"/><c:set var="label" value="Confirmed"/></c:when>
            <c:when test="${s == 'CHECKED_IN'}"><c:set var="tone" value="in"/><c:set var="label" value="Checked in"/></c:when>
            <c:when test="${s == 'CHECKED_OUT'}"><c:set var="tone" value="out"/><c:set var="label" value="Checked out"/></c:when>
            <c:otherwise><c:set var="tone" value="cancelled"/><c:set var="label" value="Cancelled"/></c:otherwise>
        </c:choose>
        <a class="bk-chip <c:if test="${selectedStatus == s}">active bk-pill-${tone}</c:if>"
           href="${ctx}/booking/list?status=${s}">
            ${label}<span class="count">${counts[s]}</span>
        </a>
    </c:forEach>
</div>

<div class="panel">
    <table class="table table-hover align-middle bk-table">
        <thead>
        <tr>
            <th>Booking</th>
            <th>Guest</th>
            <th>Room</th>
            <th>Stay</th>
            <th>Type</th>
            <th>Status</th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="b" items="${bookings}">
            <%-- Short month names, e.g. "oct" shown as "Oct" by CSS --%>
            <c:set var="fromMonth" value="${fn:toLowerCase(fn:substring(b.dateFrom.month, 0, 3))}"/>
            <c:set var="toMonth" value="${fn:toLowerCase(fn:substring(b.dateTo.month, 0, 3))}"/>
            <tr>
                <td><a class="bk-id" href="${ctx}/booking/${b.id}">${b.displayName}</a></td>
                <td>
                    <div class="bk-guest">${b.guestName}</div>
                    <div class="bk-sub">${b.guestNicPassport}</div>
                </td>
                <td class="bk-roomno">${b.roomNo}</td>
                <td>
                    <%-- Same month: "15-17 Oct", different months: "30 Oct - 2 Nov" --%>
                    <div class="bk-stay-range">
                        <c:choose>
                            <c:when test="${fromMonth == toMonth}">
                                ${b.dateFrom.dayOfMonth}-${b.dateTo.dayOfMonth} <span class="bk-month">${toMonth}</span>
                            </c:when>
                            <c:otherwise>
                                ${b.dateFrom.dayOfMonth} <span class="bk-month">${fromMonth}</span>
                                - ${b.dateTo.dayOfMonth} <span class="bk-month">${toMonth}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="bk-sub">${b.nights} ${b.nights == 1 ? 'night' : 'nights'}</div>
                </td>
                <td>${b.bookingType == 'WALK_IN' ? 'Walk-in' : 'Reservation'}</td>
                <td>
                    <c:choose>
                        <c:when test="${b.status == 'CONFIRMED'}"><span class="bk-pill bk-pill-confirmed">Confirmed</span></c:when>
                        <c:when test="${b.status == 'CHECKED_IN'}"><span class="bk-pill bk-pill-in">Checked in</span></c:when>
                        <c:when test="${b.status == 'CHECKED_OUT'}"><span class="bk-pill bk-pill-out">Checked out</span></c:when>
                        <c:otherwise><span class="bk-pill bk-pill-cancelled">Cancelled</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <%-- Only the actions allowed for this status are shown --%>
                    <div class="bk-actions">
                        <c:choose>
                            <c:when test="${b.status == 'CONFIRMED'}">
                                <form method="post" action="${ctx}/booking/${b.id}/check-in">
                                    <button class="btn btn-navy bk-btn">Check in</button>
                                </form>
                                <a class="btn bk-btn bk-btn-light" href="${ctx}/booking/${b.id}/edit">Edit</a>
                                <form method="post" action="${ctx}/booking/${b.id}/cancel"
                                      onsubmit="return confirm('Cancel booking ${b.displayName}? This cannot be undone.');">
                                    <button class="btn bk-btn bk-btn-danger">Cancel</button>
                                </form>
                            </c:when>
                            <c:when test="${b.status == 'CHECKED_IN'}">
                                <form method="post" action="${ctx}/booking/${b.id}/check-out">
                                    <button class="btn btn-navy bk-btn">Check out</button>
                                </form>
                                <a class="btn bk-btn bk-btn-light" href="${ctx}/booking/${b.id}/edit">Extend</a>
                            </c:when>
                            <c:otherwise>
                                <a class="btn bk-btn bk-btn-light" href="${ctx}/booking/${b.id}">View</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty bookings}">
            <tr>
                <td colspan="7" class="text-center text-muted py-4">No bookings found</td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
