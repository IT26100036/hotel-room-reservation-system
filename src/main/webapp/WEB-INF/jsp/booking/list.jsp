<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
  Bookings list - /booking/list
  From BookingController.list():
    bookings        list of Booking to show
    selectedStatus  chosen filter (empty = All)
    statuses        all BookingStatus values, for the filter chips
    counts          number of bookings per status, totalCount = all bookings
    success, error  one-time messages after an action
  For the walk-in window:
    guests, availableRooms   dropdown choices (rooms carry their nightly rate)
    today, tomorrow          stay starts today, earliest check-out tomorrow
    openWalkIn               true = open the window when the page loads
    walkInError + the values typed in, when "Book & check in" failed
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
    <button type="button" class="btn btn-navy" data-bs-toggle="modal" data-bs-target="#walkInModal">
        Walk-in booking
    </button>
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
                                <a class="btn bk-btn bk-btn-light" href="${ctx}/booking/${b.id}/extend">Extend</a>
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

<%-- ================= Walk-in window ================= --%>
<%-- A guest at the desk: pick or add the guest, pick a free room, book and check in at once --%>
<div class="modal fade" id="walkInModal" tabindex="-1" aria-labelledby="walkInTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-scrollable modal-lg">
        <form class="modal-content bk-modal" method="post" action="${ctx}/booking/walk-in" id="walkInForm">
            <div class="modal-header">
                <h5 class="modal-title" id="walkInTitle">Walk-in booking</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <div class="modal-body">
                <c:if test="${not empty walkInError}">
                    <div class="alert alert-danger py-2">${walkInError}</div>
                </c:if>

                <%-- ---------- guest ---------- --%>
                <div class="bk-label">Guest</div>
                <div class="bk-field mb-3">
                    <label for="guestSearch">Find existing guest</label>
                    <input class="form-control" type="search" id="guestSearch" placeholder="Search by name or NIC"
                           autocomplete="off">
                </div>

                <div class="bk-field mb-3">
                    <label for="guestChoice">Guest</label>
                    <select class="form-select" id="guestChoice" name="guestChoice" required>
                        <option value="new" ${empty guestChoiceId ? 'selected' : ''}>+ New guest (fill in below)</option>
                        <c:forEach var="g" items="${guests}">
                            <option value="${g.id}" ${g.id == guestChoiceId ? 'selected' : ''}>${g.label}</option>
                        </c:forEach>
                    </select>
                </div>

                <%-- New guest details: shown only when "+ New guest" is chosen --%>
                <div id="newGuestFields">
                    <div class="bk-field mb-3">
                        <label for="newName">Full name</label>
                        <input class="form-control" id="newName" name="newName" value="${newName}"
                               placeholder="e.g. Anjali Fernando" maxlength="100">
                    </div>
                    <div class="bk-form-grid mb-3">
                        <div class="bk-field">
                            <label for="newNic">NIC / Passport no.</label>
                            <input class="form-control mono" id="newNic" name="newNic" value="${newNic}"
                                   placeholder="199512345678" maxlength="20">
                        </div>
                        <div class="bk-field">
                            <label for="newContact">Contact no.</label>
                            <input class="form-control mono" id="newContact" name="newContact" value="${newContact}"
                                   placeholder="077 123 4567" maxlength="20">
                        </div>
                    </div>
                    <div class="bk-field mb-3">
                        <label for="newEmail">Email (optional)</label>
                        <input class="form-control" type="email" id="newEmail" name="newEmail" value="${newEmail}"
                               placeholder="guest@example.com" maxlength="100">
                    </div>
                </div>

                <hr class="bk-divider">

                <%-- ---------- stay ---------- --%>
                <div class="bk-label">Stay</div>
                <div class="bk-field mb-3">
                    <label for="walkInRoom">Room (available now)</label>
                    <select class="form-select mono" id="walkInRoom" name="roomId" required>
                        <option value="" data-rate="0">Select a room...</option>
                        <c:forEach var="r" items="${availableRooms}">
                            <option value="${r.id}" data-rate="${r.rate}" ${r.id == roomId ? 'selected' : ''}>
                                ${r.label} - Rs. <fmt:formatNumber value="${r.rate}" pattern="#,##0"/> / night
                            </option>
                        </c:forEach>
                    </select>
                    <c:if test="${empty availableRooms}">
                        <div class="bk-hint text-danger">No rooms are free tonight.</div>
                    </c:if>
                </div>

                <div class="bk-form-grid mb-3">
                    <div class="bk-field">
                        <label for="walkInFrom">Check-in</label>
                        <%-- A walk-in always starts today --%>
                        <input class="form-control mono" type="date" id="walkInFrom" value="${today}" readonly>
                    </div>
                    <div class="bk-field">
                        <label for="walkInTo">Check-out</label>
                        <input class="form-control mono" type="date" id="walkInTo" name="dateTo"
                               value="${empty dateTo ? tomorrow : dateTo}" min="${tomorrow}" required>
                    </div>
                </div>

                <div class="bk-field">
                    <label for="walkInTotal">Total amount due</label>
                    <input class="form-control mono" id="walkInTotal" value="-" readonly>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn bk-btn-light" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-navy">Book &amp; check in</button>
            </div>
        </form>
    </div>
</div>

<script>
    // Walk-in window behaviour (runs after Bootstrap has loaded)
    document.addEventListener('DOMContentLoaded', function () {
        const search = document.getElementById('guestSearch');
        const guest = document.getElementById('guestChoice');
        const newFields = document.getElementById('newGuestFields');
        const room = document.getElementById('walkInRoom');
        const from = document.getElementById('walkInFrom');
        const to = document.getElementById('walkInTo');
        const total = document.getElementById('walkInTotal');

        // Show the new-guest boxes only for "+ New guest"; required only when shown
        function toggleNewGuest() {
            const isNew = guest.value === 'new';
            newFields.style.display = isNew ? '' : 'none';
            ['newName', 'newNic', 'newContact'].forEach(function (id) {
                document.getElementById(id).required = isNew;
            });
        }

        // Typing in the search box hides guests that do not match and picks the first match
        function filterGuests() {
            const q = search.value.trim().toLowerCase();
            let first = null;
            Array.from(guest.options).forEach(function (o) {
                if (o.value === 'new') return;
                const match = q === '' || o.text.toLowerCase().includes(q);
                o.hidden = !match;
                if (match && first === null) first = o;
            });
            if (q !== '' && first) guest.value = first.value;
            if (q !== '' && !first) guest.value = 'new';
            toggleNewGuest();
        }

        // Total = nights x the room's nightly rate
        function updateTotal() {
            const rate = parseFloat(room.selectedOptions[0].dataset.rate || '0');
            const nights = Math.round((new Date(to.value) - new Date(from.value)) / 86400000);
            if (!rate || !to.value || nights < 1) {
                total.value = '-';
                return;
            }
            const amount = (rate * nights).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            total.value = 'Rs. ' + amount + '  (' + nights + (nights === 1 ? ' night' : ' nights') + ')';
        }

        search.addEventListener('input', filterGuests);
        guest.addEventListener('change', toggleNewGuest);
        room.addEventListener('change', updateTotal);
        to.addEventListener('change', updateTotal);
        toggleNewGuest();
        updateTotal();

        // Open straight away after a failed save, or when coming from /booking/new
        <c:if test="${openWalkIn}">
        new bootstrap.Modal(document.getElementById('walkInModal')).show();
        </c:if>
    });
</script>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
