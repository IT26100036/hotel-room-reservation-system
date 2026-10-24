package com.hotel.customer;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.user.staff.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final String logFilePath;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           @Value("${app.data.customer-log-file:data/customer_log.txt}") String logFilePath) {
        this.customerRepository = customerRepository;
        this.logFilePath = logFilePath;
    }

    // Generate the next ID: read all customer IDs, take the highest number, add 1, format as C001, C002...
    public synchronized String generateNextId() {
        int maxId = 0;
        for (Customer c : customerRepository.findAll()) {
            String id = c.getCustomerId();
            if (id != null && id.toUpperCase().startsWith("C")) {
                try {
                    maxId = Math.max(maxId, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("C%03d", maxId + 1);
    }

    // Register a new customer.
    public Customer register(Customer customer) {
        // 1. Generate next ID: C001, C002...
        customer.setCustomerId(generateNextId());

        // 2. Set registeredDate to today
        customer.setRegisteredDate(LocalDate.now());

        // 3. Call validate(). Each subclass adds its own NIC/passport, contact and nationality rules
        List<String> errors = new ArrayList<>(customer.validate());

        // 4. Add "already registered" if the NIC or passport is already used
        if (customer.getNicOrPassport() != null && customerRepository.countByNicOrPassport(customer.getNicOrPassport()) > 0) {
            errors.add("Customer already registered with this NIC/passport");
        }

        // 5. If the list has errors, throw ValidationException with all of them
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 6. Otherwise save
        Customer saved = customerRepository.save(customer);
        writeLog("CREATE", "Registered " + saved.getCustomerType() + " customer " + saved.getCustomerId() + " " + saved.getCustomerName());
        return saved;
    }

    // getAll() returns every customer.
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    // getById() returns the customer, or throws NotFoundException ("Customer not found").
    public Customer getById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    // search() returns everyone for an empty keyword, else matches name, NIC/passport, phone or ID.
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        return customerRepository.search(keyword.trim());
    }

    /**
     * Update an existing customer.
     * customerId, customer type, NIC/passport and registeredDate never change.
     */
    public Customer update(String customerId, Customer form) {
        // 1. Load the existing record
        Customer existing = getById(customerId);

        // 2. Copy the editable fields from the form
        existing.setCustomerName(form.getCustomerName());
        existing.setContactNo(form.getContactNo());
        existing.setEmail(form.getEmail());
        existing.setAddress(form.getAddress());
        existing.setDateOfBirth(form.getDateOfBirth());
        existing.setDiscount(form.getDiscount());
        existing.setNationality(form.getNationality()); // LocalCustomer keeps "Sri Lankan"

        // 3. Call validate()
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 4. Save
        Customer saved = customerRepository.save(existing);
        writeLog("UPDATE", "Updated customer " + saved.getCustomerId() + " " + saved.getCustomerName());
        return saved;
    }

    // Delete a customer.
    public void delete(String customerId) {
        // 1. Load the record. A wrong ID throws NotFoundException.
        Customer customer = getById(customerId);

        // 2. Remove it from the file
        customerRepository.delete(customer);
        writeLog("DELETE", "Deleted customer " + customer.getCustomerId() + " " + customer.getCustomerName());
    }

    // Returns history log lines newest first, read using BufferedReader.
    public List<String> readLog() {
        File file = new File(logFilePath);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read customer log file: " + e.getMessage());
        }

        Collections.reverse(lines); // Newest first
        return lines;
    }

    // Writes one log line using BufferedWriter in append mode.
    private void writeLog(String action, String details) {
        try {
            File file = new File(logFilePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String logLine = timestamp + " | " + action + " | " + getCurrentActorId() + " | " + details;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
                writer.write(logLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write to customer log file: " + e.getMessage());
        }
    }

    // The logged-in staff member who made the change, or SYSTEM.
    private String getCurrentActorId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Staff loggedIn = (Staff) session.getAttribute("loggedInStaff");
                    if (loggedIn != null) {
                        return loggedIn.getUserId();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "SYSTEM";
    }
}
