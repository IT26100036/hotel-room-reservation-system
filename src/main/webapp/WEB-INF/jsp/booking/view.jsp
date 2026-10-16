<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
  One booking in full - /booking/{id}
  From BookingController.view():
    booking         the Booking (WalkInBooking or ReservationBooking)
    today           today's date, for "arrives in 3 days" / "night 2 of 4"
    success, error  one-time messages after an edit or action
--%>
<jsp:include page="/WEB-INF/jsp/common/header.jsp">
    <jsp:param name="title"  value="Booking details"/>
    <jsp:param name="active" value="bookings"/>
</jsp:include>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- ---------- values worked out once, used below ---------- --%>
<c:set var="isReservation" value="${booking.bookingType == 'RESERVATION'}"/>
<c:set var="daysToArrival" value="${booking.dateFrom.toEpochDay() - today.toEpochDay()}"/>
<c:set var="nightNo" value="${today.toEpochDay() - booking.dateFrom.toEpochDay() + 1}"/>
<c:if test="${nightNo < 1}"><c:set var="nightNo" value="1"/></c:if>
<c:if test="${nightNo > booking.nights}"><c:set var="nightNo" value="${booking.nights}"/></c:if>

<%-- Status: badge style, label and where it sits on the 3-step track --%>
<c:choose>
    <c:when test="${booking.status == 'CONFIRMED'}">
        <c:set var="statusCss" value="bk-pill-confirmed"/><c:set var="statusText" value="Confirmed"/><c:set var="step" value="1"/>
    </c:when>
    <c:when test="${booking.status == 'CHECKED_IN'}">
        <c:set var="statusCss" value="bk-pill-in"/><c:set var="statusText" value="Checked in"/><c:set var="step" value="2"/>
    </c:when>
    <c:when test="${booking.status == 'CHECKED_OUT'}">
        <c:set var="statusCss" value="bk-pill-out"/><c:set var="statusText" value="Checked out"/><c:set var="step" value="3"/>
    </c:when>
    <c:otherwise>
        <c:set var="statusCss" value="bk-pill-cancelled"/><c:set var="statusText" value="Cancelled"/><c:set var="step" value="0"/>
    </c:otherwise>
</c:choose>

<%-- Stay line under the dates: what is happening right now --%>
<c:choose>
    <c:when test="${booking.status == 'CANCELLED'}">
        <c:set var="stayText" value="This booking was cancelled"/><c:set var="stayPct" value="0"/>
    </c:when>
    <c:when test="${booking.status == 'CHECKED_OUT'}">
        <c:set var="stayText" value="Stay completed"/><c:set var="stayPct" value="100"/>
    </c:when>
    <c:when test="${booking.status == 'CHECKED_IN'}">
        <c:set var="stayText" value="In house - night ${nightNo} of ${booking.nights}"/>
        <c:set var="stayPct" value="${nightNo * 100 / booking.nights}"/>
    </c:when>
    <c:when test="${daysToArrival > 1}">
        <c:set var="stayText" value="Arrives in ${daysToArrival} days"/><c:set var="stayPct" value="0"/>
    </c:when>
    <c:when test="${daysToArrival == 1}">
        <c:set var="stayText" value="Arrives tomorrow"/><c:set var="stayPct" value="0"/>
    </c:when>
    <c:when test="${daysToArrival == 0}">
        <c:set var="stayText" value="Arrives today"/><c:set var="stayPct" value="0"/>
    </c:when>
    <c:otherwise>
        <c:set var="stayText" value="Not checked in - arrival was ${-daysToArrival} day(s) ago"/><c:set var="stayPct" value="0"/>
        <c:set var="stayLate" value="true"/>
    </c:otherwise>
</c:choose>

<link rel="stylesheet" href="${ctx}/css/booking.css">

<%-- ---------- page head ---------- --%>
<div class="bk-crumb"><a href="${ctx}/booking/list">Bookings</a> / ${booking.displayName}</div>
<div class="page-head">
    <div>
        <div class="bk-title">
            <h2 class="mb-0">${booking.displayName}</h2>
            <span class="bk-pill ${statusCss}">${statusText}</span>
        </div>
        <p class="mt-1">
            <c:choose>
                <c:when test="${isReservation}">Booked from reservation <span class="mono">R-${booking.reservationId}</span></c:when>
                <c:otherwise>Walk-in booking</c:otherwise>
            </c:choose>
            &middot; ${booking.guestName} &middot; Room ${booking.roomNo}
        </p>
    </div>
    <a class="btn btn-outline-secondary btn-sm" href="${ctx}/booking/list">&larr; Back to list</a>
</div>

<%-- Messages from the last edit or action --%>
<c:if test="${not empty success}">
    <div class="alert alert-success py-2">${success}</div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger py-2">${error}</div>
</c:if>

<div class="row g-3">
    <%-- ---------- left: the stay ---------- --%>
    <div class="col-lg-8">
        <div class="bk-card">
            <div class="bk-label">Stay</div>
            <div class="bk-stay">
                <div>
                    <div class="bk-label mb-1">Check-in</div>
                    <div class="bk-date-day">${booking.dateFrom.dayOfMonth}</div>
                    <div class="bk-date-sub">
                        ${fn:toLowerCase(fn:substring(booking.dateFrom.dayOfWeek, 0, 3))},
                        ${fn:toLowerCase(fn:substring(booking.dateFrom.month, 0, 3))} ${booking.dateFrom.year}
                    </div>
                </div>
                <div class="bk-nights">
                    <span class="bk-nights-count">${booking.nights} ${booking.nights == 1 ? 'night' : 'nights'}</span>
                    <div class="bk-nights-line"></div>
                </div>
                <div class="bk-date-end">
                    <div class="bk-label mb-1">Check-out</div>
                    <div class="bk-date-day">${booking.dateTo.dayOfMonth}</div>
                    <div class="bk-date-sub">
                        ${fn:toLowerCase(fn:substring(booking.dateTo.dayOfWeek, 0, 3))},
                        ${fn:toLowerCase(fn:substring(booking.dateTo.month, 0, 3))} ${booking.dateTo.year}
                    </div>
                </div>
            </div>
            <div class="bk-progress"><span style="width: ${stayPct}%"></span></div>
            <div class="bk-stay-text ${stayLate ? 'late' : ''}">${stayText}</div>
        </div>

        <div class="bk-card">
            <div class="bk-label">Status</div>
            <c:choose>
                <c:when test="${step == 0}">
                    <div class="bk-cancel-note">
                        <strong>Cancelled.</strong> The room is free again for these dates.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="bk-track">
                        <div class="bk-step ${step >= 1 ? 'done' : ''} ${step == 1 ? 'now' : ''}">
                            <div class="bk-dot">1</div>Confirmed
                        </div>
                        <div class="bk-step ${step >= 2 ? 'done' : ''} ${step == 2 ? 'now' : ''}">
                            <div class="bk-dot">2</div>Checked in
                        </div>
                        <div class="bk-step ${step >= 3 ? 'done' : ''} ${step == 3 ? 'now' : ''}">
                            <div class="bk-dot">3</div>Checked out
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- Actions (Edit, Check in, Check out, Cancel, Delete) go here in steps 3 and 4 --%>
    </div>

    <%-- ---------- right: who, where, money ---------- --%>
    <div class="col-lg-4">
        <div class="bk-card">
            <div class="bk-label">Guest</div>
            <div class="bk-person">
                <div class="bk-avatar">${fn:substring(booking.guestName, 0, 1)}</div>
                <div>
                    <div class="bk-name">${booking.guestName}</div>
                    <div class="bk-meta">Guest #${booking.guestId}</div>
                </div>
            </div>
        </div>

        <div class="bk-card">
            <div class="bk-label">Room</div>
            <div class="bk-room">${booking.roomNo}</div>
            <div class="bk-meta mt-1">Room #${booking.roomId}</div>
        </div>

        <div class="bk-card bk-gold">
            <div class="bk-label">Payment</div>
            <c:choose>
                <%-- Only a ReservationBooking has reservationId and advancePaid --%>
                <c:when test="${isReservation}">
                    <div class="bk-row">
                        <span class="k">Reservation</span>
                        <span class="mono">R-${booking.reservationId}</span>
                    </div>
                    <div class="bk-row">
                        <span class="k">Advance paid</span>
                        <span class="bk-money">Rs. <fmt:formatNumber value="${booking.advancePaid}" pattern="#,##0.00"/></span>
                    </div>
                    <div class="bk-row">
                        <span class="k">Due at check-out</span>
                        <span>Room total less advance</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="bk-row">
                        <span class="k">Advance paid</span>
                        <span class="bk-money">Rs. 0.00</span>
                    </div>
                    <div class="bk-row">
                        <span class="k">Due at check-out</span>
                        <span>Full room total</span>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
