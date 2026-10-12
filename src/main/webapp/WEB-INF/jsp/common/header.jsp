<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Shared layout - top part. Put this at the top of every page:

  <jsp:include page="/WEB-INF/jsp/common/header.jsp">
      <jsp:param name="title"  value="Rooms"/>
      <jsp:param name="active" value="rooms"/>
  </jsp:include>

  active = dashboard | reservations | bookings | checkinout | rooms | guests | finance | staff
--%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${empty param.title ? 'Harbourline' : param.title} | Harbourline Hotel</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/images/logo.svg">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&family=IBM+Plex+Sans:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/style.css" rel="stylesheet">
</head>
<body>
<div class="app">
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp"/>
    <div class="app-main">
        <jsp:include page="/WEB-INF/jsp/common/topbar.jsp"/>
        <main class="app-content">
