<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${param.active}"/>
<c:set var="staff" value="${sessionScope.loggedStaff}"/>

<aside class="sidebar">
    <a class="brand" href="${ctx}/">
        <img src="${ctx}/images/logo.svg" width="34" height="34" alt="Harbourline logo">
        <span>
            <span class="brand-name">Harbourline</span>
            <span class="brand-sub">HOTEL &middot; GALLE FORT</span>
        </span>
    </a>

    <nav class="side-nav">
        <div class="side-group">FRONT OFFICE</div>
        <a class="side-link ${active == 'dashboard' ? 'active' : ''}" href="${ctx}/">Dashboard</a>
        <a class="side-link ${active == 'reservations' ? 'active' : ''}" href="${ctx}/reservation/list">Reservations</a>
        <a class="side-link ${active == 'bookings' ? 'active' : ''}" href="${ctx}/booking/list">Bookings</a>
        <a class="side-link ${active == 'checkinout' ? 'active' : ''}" href="${ctx}/occupancy/list">Check-in / out</a>

        <div class="side-group">PROPERTY</div>
        <a class="side-link ${active == 'rooms' ? 'active' : ''}" href="${ctx}/room/list">Rooms</a>
        <a class="side-link ${active == 'guests' ? 'active' : ''}" href="${ctx}/guest/list">Guests</a>

        <div class="side-group">BACK OFFICE</div>
        <a class="side-link ${active == 'finance' ? 'active' : ''}" href="${ctx}/finance/payments">Finance</a>
        <%-- Staff is admin only. Until login is built (no loggedStaff yet) it stays visible for testing. --%>
        <c:if test="${empty staff or staff.role == 'ADMIN'}">
            <a class="side-link ${active == 'staff' ? 'active' : ''}" href="${ctx}/admin/list">Staff</a>
        </c:if>
    </nav>

    <div class="side-user">
        <div>
            <div class="side-user-name">${empty staff ? 'Not signed in' : staff.fullName}</div>
            <div class="side-user-role">${empty staff ? '-' : (staff.role == 'ADMIN' ? 'Admin' : 'Receptionist')}</div>
        </div>
        <c:choose>
            <c:when test="${empty staff}">
                <a class="btn btn-sm btn-outline-secondary" href="${ctx}/admin/login">Sign in</a>
            </c:when>
            <c:otherwise>
                <a class="btn btn-sm btn-outline-secondary" href="${ctx}/admin/logout">Log out</a>
            </c:otherwise>
        </c:choose>
    </div>
</aside>
