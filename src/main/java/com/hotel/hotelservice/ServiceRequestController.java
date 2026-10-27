package com.hotel.hotelservice;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.customer.CustomerService;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;
import com.hotel.hotelservice.enums.TripType;
import com.hotel.hotelservice.enums.VehicleType;
import com.hotel.hotelservice.types.FoodRequest;
import com.hotel.hotelservice.types.LaundryRequest;
import com.hotel.hotelservice.types.TransportRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/services")
public class ServiceRequestController {

    private final ServiceRequestService requestService;
    private final CustomerService customerService;

    public ServiceRequestController(ServiceRequestService requestService, CustomerService customerService) {
        this.requestService = requestService;
        this.customerService = customerService;
    }

    /**
     * ServiceRequest is abstract, so Spring can't create one from the form by itself.
     * This creates a FoodRequest, LaundryRequest or TransportRequest from the "serviceType" value,
     * then Spring fills in the form fields on that object.
     */
    @ModelAttribute("serviceRequest")
    public ServiceRequest requestFromType(@RequestParam(value = "serviceType", required = false) String serviceType) {
        if ("LAUNDRY".equalsIgnoreCase(serviceType)) {
            return new LaundryRequest();
        }
        if ("TRANSPORT".equalsIgnoreCase(serviceType)) {
            return new TransportRequest();
        }
        return new FoodRequest();
    }

    /* ---------- List and details ---------- */

    @GetMapping
    public String listRequests(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", required = false) RequestStatus status,
                               Model model) {
        List<ServiceRequest> requests = (status != null) ? requestService.getByStatus(status) : requestService.search(keyword);
        model.addAttribute("requestList", requests);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", RequestStatus.values());
        return "services/list";
    }

    @GetMapping("/view/{id}")
    public String viewRequest(@PathVariable("id") String id, Model model) {
        ServiceRequest request = requestService.getById(id);
        model.addAttribute("serviceRequest", request);
        try {
            model.addAttribute("customer", customerService.getById(request.getCustomerId()));
        } catch (NotFoundException ignored) {
        }
        return "services/details";
    }

    /* ---------- Create ---------- */

    /** /services/create?serviceType=LAUNDRY shows the laundry form, and so on */
    @GetMapping("/create")
    public String showCreateForm(@ModelAttribute("serviceRequest") ServiceRequest request, Model model) {
        if (request instanceof TransportRequest transport) {
            transport.setTravelDate(LocalDate.now());
        }
        addFormOptions(model, false);
        return "services/form";
    }

    @PostMapping("/create")
    public String createRequest(@ModelAttribute("serviceRequest") ServiceRequest request, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            ServiceRequest saved = requestService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Request " + saved.getRequestId() + " recorded successfully!");
            return "redirect:/services/view/" + saved.getRequestId();
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            addFormOptions(model, false);
            return "services/form";
        }
    }

    /* ---------- Update ---------- */

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("serviceRequest", requestService.getById(id));
        addFormOptions(model, true);
        return "services/form";
    }

    @PostMapping("/edit/{id}")
    public String updateRequest(@PathVariable("id") String id, @ModelAttribute("serviceRequest") ServiceRequest form,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            requestService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Request updated successfully!");
            return "redirect:/services/view/" + id;
        } catch (ValidationException e) {
            ServiceRequest existing = requestService.getById(id);
            form.setRequestId(existing.getRequestId());
            form.setCustomerId(existing.getCustomerId());
            form.setStatus(existing.getStatus());
            model.addAttribute("serviceRequest", form);
            model.addAttribute("errors", e.getErrors());
            addFormOptions(model, true);
            return "services/form";
        }
    }

    /** Moves the request to IN_PROGRESS, COMPLETED or CANCELLED */
    @PostMapping("/status/{id}")
    public String changeStatus(@PathVariable("id") String id, @RequestParam("status") RequestStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            requestService.changeStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Request " + id + " is now " + status + ".");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/services/view/" + id;
    }

    /* ---------- Delete ---------- */

    @PostMapping("/delete/{id}")
    public String deleteRequest(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            requestService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request " + id + " deleted.");
            return "redirect:/services";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/services/view/" + id;
        }
    }

    /* ---------- History ---------- */

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("logs", requestService.readLog());
        return "services/history";
    }

    /** Lists for the form's drop-downs */
    private void addFormOptions(Model model, boolean isEdit) {
        model.addAttribute("customers", customerService.getAll());
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("tripTypes", TripType.values());
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("isEdit", isEdit);
    }
}