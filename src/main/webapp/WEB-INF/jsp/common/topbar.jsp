<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<header class="topbar">
    <h1 class="topbar-title">${empty param.title ? 'Dashboard' : param.title}</h1>
    <form class="topbar-search" action="${pageContext.request.contextPath}/search" method="get">
        <input type="search" name="q" placeholder="Search guest, room or ID" aria-label="Search">
    </form>
    <div class="topbar-clock">
        <div class="clock-time" id="clockTime">--:--:--</div>
        <div class="clock-date" id="clockDate"></div>
    </div>
</header>
