<%@ page contentType="text/html;charset=UTF-8" %>
<footer class="site-footer">
    <div class="container">
        <div class="row gy-4 align-items-start">
            <div class="col-md-6">
                <a class="footer-brand" href="${pageContext.request.contextPath}/">
                    <span class="footer-brand-mark" aria-hidden="true">H</span>
                    <span>Hotel Operations</span>
                </a>
                <p class="footer-description">
                    Internal workspace for hotel reception and administrative staff.
                </p>
            </div>
            <div class="col-6 col-md-3">
                <h2 class="footer-heading">Front desk</h2>
                <ul class="footer-links">
                    <li><a href="${pageContext.request.contextPath}/room">Room inventory</a></li>
                    <li><a href="${pageContext.request.contextPath}/reservation">Reservations</a></li>
                    <li><a href="${pageContext.request.contextPath}/booking">Bookings</a></li>
                </ul>
            </div>
            <div class="col-6 col-md-3">
                <h2 class="footer-heading">Administration</h2>
                <ul class="footer-links">
                    <li><a href="${pageContext.request.contextPath}/admin">Staff management</a></li>
                    <li><a href="${pageContext.request.contextPath}/occupancy">Guest stays</a></li>
                    <li><a href="${pageContext.request.contextPath}/finance">Payments &amp; finance</a></li>
                </ul>
            </div>
        </div>
        <div class="footer-bottom">
            <span>Hotel Room Reservation System</span>
            <span>Staff operations portal</span>
        </div>
    </div>
</footer>
