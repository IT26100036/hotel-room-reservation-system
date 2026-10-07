<%@ page contentType="text/html;charset=UTF-8" %>
<header class="site-header">
    <nav class="navbar navbar-expand-lg navbar-dark" aria-label="Main navigation">
        <div class="container">
            <a class="navbar-brand site-brand" href="${pageContext.request.contextPath}/">
                <span class="site-brand-mark" aria-hidden="true">H</span>
                <span class="site-brand-copy">
                    <span class="site-brand-name">Hotel Operations</span>
                    <span class="site-brand-caption">Staff workspace</span>
                </span>
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                    data-bs-target="#mainNav" aria-controls="mainNav"
                    aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="mainNav">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/admin">Staff</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/room">Rooms</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/reservation">Reservations</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/booking">Bookings</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/occupancy">Guest stays</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/finance">Finance</a></li>
                </ul>
                <div class="dropdown profile-menu">
                    <button class="btn profile-toggle dropdown-toggle" type="button"
                            id="profileMenu" data-bs-toggle="dropdown" aria-expanded="false"
                            aria-label="Open profile menu">
                        <span class="profile-avatar" aria-hidden="true">
                            <svg viewBox="0 0 24 24" focusable="false">
                                <circle cx="12" cy="8" r="4"></circle>
                                <path d="M4.5 20c.5-4 3.2-6 7.5-6s7 2 7.5 6"></path>
                            </svg>
                        </span>
                        <span class="profile-label">Profile</span>
                    </button>
                    <div class="dropdown-menu dropdown-menu-end profile-dropdown" aria-labelledby="profileMenu">
                        <form action="${pageContext.request.contextPath}/logout" method="post">
                            <button class="dropdown-item signout-action" type="submit">
                                <span aria-hidden="true">&#10140;</span>
                                Sign out
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </nav>
</header>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" defer></script>
