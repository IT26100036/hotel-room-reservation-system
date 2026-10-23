package com.hotel.user.staff;

import com.hotel.user.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    @Autowired
    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public String listStaff(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Staff> staffList = staffService.search(keyword);
        model.addAttribute("staffList", staffList);
        model.addAttribute("keyword", keyword);
        return "staff/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("staff")) {
            Staff staff = new Staff();
            staff.setStatus(Status.ACTIVE);
            model.addAttribute("staff", staff);
        }
        model.addAttribute("staffTypes", StaffType.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("isEdit", false);
        return "staff/form";
    }

    @PostMapping("/create")
    public String createStaff(@ModelAttribute("staff") Staff staff,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            staffService.create(staff);
            redirectAttributes.addFlashAttribute("successMessage", "Staff member created successfully!");
            return "redirect:/staff";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("staff", staff);
            model.addAttribute("staffTypes", StaffType.values());
            model.addAttribute("statuses", Status.values());
            model.addAttribute("isEdit", false);
            return "staff/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Staff staff = staffService.getById(id);
        model.addAttribute("staff", staff);
        model.addAttribute("staffTypes", StaffType.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("isEdit", true);
        return "staff/form";
    }

    @PostMapping("/edit/{id}")
    public String updateStaff(@PathVariable("id") String id,
                              @ModelAttribute("staff") Staff staff,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            staffService.update(id, staff);
            redirectAttributes.addFlashAttribute("successMessage", "Staff member updated successfully!");
            return "redirect:/staff";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            staff.setUserId(id);
            model.addAttribute("staff", staff);
            model.addAttribute("staffTypes", StaffType.values());
            model.addAttribute("statuses", Status.values());
            model.addAttribute("isEdit", true);
            return "staff/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteStaff(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            staffService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Staff member deleted successfully!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff";
    }

    @GetMapping("/history")
    public String viewHistory(Model model) {
        List<String> logs = staffService.readLog();
        model.addAttribute("logs", logs);
        return "staff/history";
    }
}
