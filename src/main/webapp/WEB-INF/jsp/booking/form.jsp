<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Booking form - one page used two ways (mode from BookingController):
    edit    /booking/{id}/edit    confirmed booking: room and dates (guest locked)
    extend  /booking/{id}/extend  checked-in booking: only check-out can move later
  Also from the controller:
    bookingId, guestId, roomId, dateFrom, dateTo   values for the boxes
    guests, rooms                                  dropdown choices (OptionItem)
    error                                          message when saving failed
--%>
<c:choose>
    <c:when test="${mode == 'edit'}">
        <c:set var="pageTitle" value="Edit booking B-${bookingId}"/>
        <c:set var="intro" value="Change the room or dates. The guest stays the same."/>
        <c:set var="saveText" value="Save changes"/>
        <c:set var="formAction" value="/booking/${bookingId}/edit"/>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="Extend stay B-${bookingId}"/>
        <c:set var="intro" value="The guest is in the room. Pick a later check-out date."/>
        <c:set var="saveText" value="Extend stay"/>
        <c:set var="formAction" value="/booking/${bookingId}/extend"/>
    </c:otherwise>
</c:choose>
<c:set var="isExtend" value="${mode == 'extend'}"/>

<jsp:include page="/WEB-INF/jsp/common/header.jsp">
    <jsp:param name="title"  value="${pageTitle}"/>
    <jsp:param name="active" value="bookings"/>
</jsp:include>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<link rel="stylesheet" href="${ctx}/css/booking.css">

<div class="bk-crumb">
    <a href="${ctx}/booking/list">Bookings</a>
    / <a href="${ctx}/booking/${bookingId}">B-${bookingId}</a>
    / ${isExtend ? 'Extend' : 'Edit'}
</div>
<div class="page-head">
    <div>
        <h2>${pageTitle}</h2>
        <p>${intro}</p>
    </div>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger py-2">${error}</div>
</c:if>

<div class="row g-3">
    <%-- ---------- left: the form ---------- --%>
    <div class="col-lg-8">
        <form class="bk-card" method="post" action="${ctx}${formAction}">
            <div class="bk-label">Booking details</div>
            <div class="bk-form-grid">

                <%-- Guest: always locked, a booking cannot move to another guest --%>
                <div class="bk-field bk-field-full">
                    <label for="guestId">Guest</label>
                    <select class="form-select" id="guestId" name="guestId" disabled>
                        <option value="">Choose a guest...</option>
                        <c:forEach var="g" items="${guests}">
                            <option value="${g.id}" ${g.id == guestId ? 'selected' : ''}>${g.label}</option>
                        </c:forEach>
                    </select>
                    <%-- A disabled box is not sent, so send the value hidden --%>
                    <input type="hidden" name="guestId" value="${guestId}">
                    <div class="bk-hint">A booking cannot be moved to another guest.</div>
                </div>

                <%-- Room: locked when extending (the guest is already in it) --%>
                <div class="bk-field bk-field-full">
                    <label for="roomId">Room</label>
                    <select class="form-select" id="roomId" name="roomId" ${isExtend ? 'disabled' : 'required'}>
                        <option value="">Choose a room...</option>
                        <c:forEach var="r" items="${rooms}">
                            <option value="${r.id}" ${r.id == roomId ? 'selected' : ''}>${r.label}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${isExtend}"><input type="hidden" name="roomId" value="${roomId}"></c:if>
                    <div class="bk-hint">Rooms under maintenance are not listed. Free dates are checked when you save.</div>
                </div>

                <div class="bk-field">
                    <label for="dateFrom">Check-in</label>
                    <input class="form-control" type="date" id="dateFrom" name="dateFrom" value="${dateFrom}"
                           ${isExtend ? 'readonly' : 'required'} <c:if test="${not isExtend}">min="${today}"</c:if>>
                </div>

                <div class="bk-field">
                    <label for="dateTo">Check-out</label>
                    <input class="form-control" type="date" id="dateTo" name="dateTo" value="${dateTo}" required>
                </div>
            </div>

            <div class="bk-form-actions">
                <button type="submit" class="btn btn-navy">${saveText}</button>
                <a class="btn bk-btn-light" href="${ctx}/booking/${bookingId}">Back</a>
            </div>
        </form>
    </div>

    <%-- ---------- right: live summary and help ---------- --%>
    <div class="col-lg-4">
        <div class="bk-card">
            <div class="bk-label">Stay</div>
            <div class="bk-summary-nights" id="nightsCount">-</div>
            <div class="bk-summary-unit" id="nightsText">Pick check-in and check-out</div>
        </div>
        <div class="bk-card bk-gold">
            <div class="bk-label">Before you save</div>
            <ol class="bk-steps">
                <c:choose>
                    <c:when test="${isExtend}">
                        <li>The new check-out must be after the current one.</li>
                        <li>The room must be free for the extra nights.</li>
                    </c:when>
                    <c:otherwise>
                        <li>Check-in cannot be in the past.</li>
                        <li>Check-out must be after check-in.</li>
                        <li>The room must be free for every night.</li>
                    </c:otherwise>
                </c:choose>
            </ol>
        </div>
    </div>
</div>

<script>
    // Live night counter: updates as the dates change
    (function () {
        const from = document.getElementById('dateFrom');
        const to = document.getElementById('dateTo');
        const count = document.getElementById('nightsCount');
        const text = document.getElementById('nightsText');

        function update() {
            // Check-out cannot be picked before the day after check-in
            if (from.value) {
                const next = new Date(from.value);
                next.setDate(next.getDate() + 1);
                to.min = next.toISOString().slice(0, 10);
            }
            if (!from.value || !to.value) {
                count.textContent = '-';
                text.textContent = 'Pick check-in and check-out';
                return;
            }
            const nights = Math.round((new Date(to.value) - new Date(from.value)) / 86400000);
            if (nights < 1) {
                count.textContent = '-';
                text.textContent = 'Check-out must be after check-in';
                return;
            }
            count.textContent = nights;
            text.textContent = nights === 1 ? 'night' : 'nights';
        }

        from.addEventListener('change', update);
        to.addEventListener('change', update);
        update();
    })();
</script>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
