package com.hotel.customer;

import com.hotel.common.ValidationException;
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

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Customer is abstract, so Spring can't create one from the form by itself.
     * This runs before each request and creates the right subclass from the
     * "customerType" value, then Spring fills in the form fields on that object.
     */
    @ModelAttribute("customer")
    public Customer customerFromType(@RequestParam(value = "customerType", required = false) String customerType) {
        if ("FOREIGN".equalsIgnoreCase(customerType)) {
            return new ForeignCustomer();
        }
        return new LocalCustomer();
    }

    @GetMapping
    public String listCustomers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("customerList", customerService.search(keyword));
        model.addAttribute("keyword", keyword);
        return "customer/list";
    }

    @GetMapping("/view/{id}")
    public String viewCustomer(@PathVariable("id") String id, Model model) {
        model.addAttribute("customer", customerService.getById(id));
        return "customer/details";
    }

    @GetMapping("/create")
    public String showCreateForm(@ModelAttribute("customer") Customer customer, Model model) {
        model.addAttribute("isEdit", false);
        return "customer/form";
    }

    @PostMapping("/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            Customer saved = customerService.register(customer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer " + saved.getCustomerId() + " registered successfully!");
            return "redirect:/customer";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("isEdit", false);
            return "customer/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isEdit", true);
        return "customer/form";
    }

    @PostMapping("/edit/{id}")
    public String updateCustomer(@PathVariable("id") String id,
                                 @ModelAttribute("customer") Customer customer,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            customerService.update(id, customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
            return "redirect:/customer/view/" + id;
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            customer.setCustomerId(id);
            model.addAttribute("isEdit", true);
            return "customer/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            customerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Customer deleted successfully!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer";
    }

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("logs", customerService.readLog());
        return "customer/history";
    }
}
