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
    <%--    content--%>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
