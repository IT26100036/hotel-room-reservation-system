package com.hotel.hotelservice;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.customer.CustomerService;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.user.staff.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hotel services CRUD: food, laundry and transport requests.
 * Uses Customer (customer must exist).
 */
@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final CustomerService customerService;
    private final String logFilePath;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ServiceRequestService(ServiceRequestRepository requestRepository,
                                 CustomerService customerService,
                                 @Value("${app.data.service-log-file:data/service_log.txt}") String logFilePath) {
        this.requestRepository = requestRepository;
        this.customerService = customerService;
        this.logFilePath = logFilePath;
    }

    /** Next ID: finds the highest number and adds 1 -> SR001, SR002... */
    public synchronized String generateNextId() {
        int max = 0;
        for (ServiceRequest request : requestRepository.findAll()) {
            String id = request.getRequestId();
            if (id != null && id.toUpperCase().startsWith("SR")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(2)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("SR%03d", max + 1);
    }

    /* ---------------- Create ---------------- */

    /** Records a food, laundry or transport request (the form decided which subclass) */
    public synchronized ServiceRequest create(ServiceRequest request) {
        // 1. Values the system sets
        request.setRequestId(generateNextId());
        request.setRequestDate(LocalDate.now());
        request.setStatus(RequestStatus.REQUESTED);

        // 2. Rules of the request and of its own type
        List<String> errors = new ArrayList<>(request.validate());

        // 3. Customer must exist
        checkCustomer(request, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        request.setCustomerId(request.getCustomerId().trim().toUpperCase());
        ServiceRequest saved = requestRepository.save(request);
        writeLog("CREATE", String.format("Created %s request %s for %s: %s (LKR %.2f)",
                saved.getServiceType(), saved.getRequestId(), saved.getCustomerId(),
                saved.getSummary(), saved.calculateCharge()));
        return saved;
    }

    /* ---------------- Read ---------------- */

    public List<ServiceRequest> getAll() {
        return requestRepository.findAll();
    }

    public ServiceRequest getById(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    /** Empty keyword -> all requests, else search by request ID, customer ID or service type */
    public List<ServiceRequest> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        return requestRepository.search(keyword);
    }

    public List<ServiceRequest> getByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    /** Total of a customer's COMPLETED services (Finance can use this for extra charges later) */
    public double getCompletedTotalForCustomer(String customerId) {
        return requestRepository.findByCustomerId(customerId).stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED)
                .mapToDouble(ServiceRequest::calculateCharge)
                .sum();
    }

    /* ---------------- Update ---------------- */

    /** Changes the details of a request that has not started. ID, type and customer never change. */
    public synchronized ServiceRequest update(String requestId, ServiceRequest form) {
        ServiceRequest existing = getById(requestId);

        if (!existing.isEditable()) {
            throw new ValidationException("Only a request that has not started can be changed");
        }
        if (form.getServiceType() != existing.getServiceType()) {
            throw new ValidationException("Service type cannot be changed");
        }

        // Each type copies its own fields (polymorphism)
        existing.copyDetailsFrom(form);
        existing.setNotes(form.getNotes());

        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        ServiceRequest saved = requestRepository.save(existing);
        writeLog("UPDATE", String.format("Updated request %s: %s (LKR %.2f)",
                saved.getRequestId(), saved.getSummary(), saved.calculateCharge()));
        return saved;
    }

    /** Moves the status forward: REQUESTED -> IN_PROGRESS -> COMPLETED, or CANCELLED */
    public ServiceRequest changeStatus(String requestId, RequestStatus newStatus) {
        ServiceRequest request = getById(requestId);

        if (newStatus == null || !request.canMoveTo(newStatus)) {
            throw new ValidationException("Cannot change a " + request.getStatus() + " request to " + newStatus);
        }

        request.setStatus(newStatus);
        requestRepository.save(request);
        writeLog("STATUS", "Request " + request.getRequestId() + " is now " + newStatus);
        return request;
    }

    /* ---------------- Delete ---------------- */

    /** Completed requests are kept because they are billed */
    public void delete(String requestId) {
        ServiceRequest request = getById(requestId);
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new ValidationException("Completed requests cannot be deleted");
        }
        requestRepository.delete(request);
        writeLog("DELETE", "Deleted request " + request.getRequestId());
    }

    /* ---------------- Helpers ---------------- */

    private void checkCustomer(ServiceRequest request, List<String> errors) {
        if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
            return;   // "Customer is required" already added by validate()
        }
        try {
            customerService.getById(request.getCustomerId().trim());
        } catch (NotFoundException e) {
            errors.add("Customer not found");
        }
    }

    /** All log lines, newest first */
    public List<String> readLog() {
        List<String> lines = new ArrayList<>();
        File file = new File(logFilePath);
        if (!file.exists()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read service log: " + e.getMessage());
        }
        Collections.reverse(lines);
        return lines;
    }

    /** Adds one line to the log (append mode): time | action | staff ID | details */
    private void writeLog(String action, String details) {
        try {
            File file = new File(logFilePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            String line = LocalDateTime.now().format(TIME_FORMAT) + " | " + action + " | "
                    + getCurrentStaffId() + " | " + details;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write service log: " + e.getMessage());
        }
    }

    /** ID of the logged-in staff member, or SYSTEM */
    private String getCurrentStaffId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Staff staff = (Staff) session.getAttribute("loggedInStaff");
                    if (staff != null) {
                        return staff.getUserId();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "SYSTEM";
    }
}
