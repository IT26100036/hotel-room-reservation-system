package com.hotel.config;

import com.hotel.user.staff.Staff;
import com.hotel.user.staff.StaffType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        // Login page and CSS/JS/static assets are skipped
        if (path.equals("/login") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/error") || path.equals("/favicon.ico")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Staff loggedInStaff = (session != null) ? (Staff) session.getAttribute("loggedInStaff") : null;

        // If there's no "loggedInStaff" in the session, send user to login page
        if (loggedInStaff == null) {
            response.sendRedirect(contextPath + "/login");
            return false;
        }

        // Creating, editing and deleting staff and rooms is only for ADMIN
        boolean isAdminMutation = path.startsWith("/staff/create")
                || path.startsWith("/staff/edit")
                || path.startsWith("/staff/delete")
                || path.startsWith("/room/create")
                || path.startsWith("/room/edit")
                || path.startsWith("/room/delete")
                || path.startsWith("/finance/invoice/void");

        if (isAdminMutation && loggedInStaff.getStaffType() != StaffType.ADMIN) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<!DOCTYPE html><html><head><title>Access Denied</title>"
                    + "<link rel='stylesheet' href='" + contextPath + "/css/style.css'>"
                    + "</head><body class='permission-denied-body'>"
                    + "<div class='permission-denied-card'>"
                    + "<h2>Access Denied</h2>"
                    + "<p class='error-text'>You don't have permission to perform this action. Administrator privileges required.</p>"
                    + "<a href='" + contextPath + "/staff' class='btn btn-primary'>Return to Staff Directory</a>"
                    + "</div></body></html>");
            return false;
        }

        return true;
    }
}
