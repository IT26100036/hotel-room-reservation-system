package com.hotel.user;

import com.hotel.user.staff.Staff;
import com.hotel.user.staff.StaffService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final StaffService staffService;

    @Autowired
    public AuthController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        Staff staff = (Staff) session.getAttribute("loggedInStaff");
        if (staff == null) {
            return "redirect:/login";
        }
        return "redirect:/staff";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session != null && session.getAttribute("loggedInStaff") != null) {
            return "redirect:/staff";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        try {
            Staff staff = staffService.login(username, password);
            // On success, save the staff member in the HttpSession as "loggedInStaff"
            session.setAttribute("loggedInStaff", staff);
            return "redirect:/staff";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            Staff staff = (Staff) session.getAttribute("loggedInStaff");
            if (staff != null) {
                staffService.logout(staff);
            }
            // Logout clears the session and goes back to the login page
            session.invalidate();
        }
        return "redirect:/login";
    }
}
