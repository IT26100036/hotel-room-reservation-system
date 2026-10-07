<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reception Dashboard | Hotel Room Reservation System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="common/header.jsp"/>

<main class="home-page">
    <div class="container py-4 py-lg-5">
        <section class="dashboard-welcome" aria-labelledby="welcomeTitle">
            <div>
                <span class="welcome-eyebrow">RECEPTION WORKSPACE</span>
                <h1 id="welcomeTitle">Front desk dashboard</h1>
                <p>Manage guest reservations, room availability, check-ins, and payments from one place.</p>
            </div>
            <div class="dashboard-actions">
                <a class="btn dashboard-primary" href="${pageContext.request.contextPath}/booking">
                    <span aria-hidden="true">+</span> New booking
                </a>
                <a class="btn dashboard-secondary" href="${pageContext.request.contextPath}/reservation">
                    Find a reservation
                </a>
            </div>
        </section>

        <section class="module-section" aria-labelledby="modulesTitle">
            <div class="section-heading">
                <div>
                    <span class="section-eyebrow">HOTEL OPERATIONS</span>
                    <h2 id="modulesTitle">Front desk tools</h2>
                </div>
                <p>Select a section to continue.</p>
            </div>

            <div class="row g-3 g-xl-4">
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/room">
                        <span class="module-icon icon-rooms" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><path d="M4 20V5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5V20M3 20h18M7 8h4v4H7zM13 8h4v4h-4zM8 20v-3h8v3"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Room inventory</span>
                            <span class="module-card-description">Review rooms and update availability.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/reservation">
                        <span class="module-icon icon-reservations" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2"></rect><path d="M8 3v4M16 3v4M4 10h16M8 14h3M8 17h6"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Reservations</span>
                            <span class="module-card-description">Find and manage advance reservations.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/booking">
                        <span class="module-icon icon-bookings" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><path d="M4 19V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v11M3 19h18M7 10h4v3H7zM13 10h4M13 13h4M8 19v-3h8v3"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Bookings</span>
                            <span class="module-card-description">Create and manage guest bookings.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/occupancy">
                        <span class="module-icon icon-occupancy" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><path d="M4 20V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12M3 20h18M7 10h4v4H7zM13 10h4M13 13h4M8 20v-3h8v3"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Check-in &amp; check-out</span>
                            <span class="module-card-description">Process guest arrivals and departures.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/finance">
                        <span class="module-icon icon-finance" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="14" rx="2"></rect><path d="M4 10h16M8 15h3M15 14v3M13.5 15.5h3"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Payments &amp; finance</span>
                            <span class="module-card-description">Review payments and guest invoices.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
                <div class="col-sm-6 col-xl-4">
                    <a class="module-card" href="${pageContext.request.contextPath}/admin">
                        <span class="module-icon icon-admin" aria-hidden="true">
                            <svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3"></circle><path d="M3.5 19v-1.5A4.5 4.5 0 0 1 8 13h2a4.5 4.5 0 0 1 4.5 4.5V19M16 5.5a3 3 0 0 1 0 5.8M17 14h.5a3 3 0 0 1 3 3v2"></path></svg>
                        </span>
                        <span class="module-card-copy">
                            <span class="module-card-title">Staff administration</span>
                            <span class="module-card-description">Manage staff accounts and access.</span>
                        </span>
                        <span class="module-arrow" aria-hidden="true">&rarr;</span>
                    </a>
                </div>
            </div>
        </section>
    </div>
</main>

<jsp:include page="common/footer.jsp"/>

</body>
</html>
