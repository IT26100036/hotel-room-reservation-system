package com.hotel.user.staff;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final String logFilePath;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public StaffService(StaffRepository staffRepository,
                        @Value("${app.data.staff-log-file:data/staff_log.txt}") String logFilePath) {
        this.staffRepository = staffRepository;
        this.logFilePath = logFilePath;
    }

    /**
     * Generate the next ID: read all staff IDs, take the highest number, add 1, format as S001, S002...
     */
    public synchronized String generateNextId() {
        List<Staff> allStaff = staffRepository.findAll();
        int maxId = 0;
        for (Staff s : allStaff) {
            String uid = s.getUserId();
            if (uid != null && uid.toUpperCase().startsWith("S")) {
                try {
                    int num = Integer.parseInt(uid.substring(1));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("S%03d", maxId + 1);
    }

    /**
     * Create staff member according to specifications.
     */
    public Staff create(Staff staff) {
        // 1. Generate next ID: S001, S002...
        staff.setUserId(generateNextId());

        // 2. Set status to ACTIVE
        staff.setStatus(Status.ACTIVE);

        // If staffType is EMPLOYEE, username and password must be empty (NULL)
        if (staff.getStaffType() == StaffType.EMPLOYEE) {
            staff.setUsername(null);
            staff.setPassword(null);
        }

        // 3. Call staff.validate() to get the list of rule errors
        List<String> errors = new ArrayList<>(staff.validate());

        // 4. Add "NIC already registered" if countByNic is above 0, and "Username already exists" if existsByUsername is true
        if (staff.getNicOrPassport() != null && staffRepository.countByNic(staff.getNicOrPassport()) > 0) {
            errors.add("NIC already registered");
        }

        if (staff.getUsername() != null && !staff.getUsername().trim().isEmpty() && staffRepository.existsByUsername(staff.getUsername())) {
            errors.add("Username already exists");
        }

        // 5. If the list has errors, throw ValidationException with all of them, so the form shows every error at once
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 6. Otherwise save(staff). The password is saved as typed (plain text).
        Staff saved = staffRepository.save(staff);

        // Write log
        writeLog("CREATE", "Added staff " + saved.getUserId() + " " + saved.getName());

        return saved;
    }

    /**
     * getAll() returns findAll().
     */
    public List<Staff> getAll() {
        return staffRepository.findAll();
    }

    /**
     * getById() returns the staff member, or throws NotFoundException ("Staff member not found").
     */
    public Staff getById(String userId) {
        return staffRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Staff member not found"));
    }

    /**
     * search() returns everyone for an empty keyword, else matches part of the name or NIC (case-insensitive).
     */
    public List<Staff> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        return staffRepository.search(keyword.trim());
    }

    /**
     * Update existing staff member.
     */
    public Staff update(String userId, Staff form) {
        // 1. Load the existing record with getById(userId)
        Staff existing = getById(userId);

        // 2. Copy the editable fields from the form: name, contactNo, email, address, staffType, position, salary, joinedDate, username, status
        existing.setName(form.getName());
        existing.setContactNo(form.getContactNo());
        existing.setEmail(form.getEmail());
        existing.setAddress(form.getAddress());
        existing.setStaffType(form.getStaffType());
        existing.setPosition(form.getPosition());
        existing.setSalary(form.getSalary());
        existing.setJoinedDate(form.getJoinedDate());
        existing.setUsername(form.getUsername());
        existing.setStatus(form.getStatus());

        // 3. Copy the password only if a new one was typed. A blank password keeps the old one.
        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            existing.setPassword(form.getPassword());
        }

        // 4. If staffType is EMPLOYEE, set username and password to NULL
        if (existing.getStaffType() == StaffType.EMPLOYEE) {
            existing.setUsername(null);
            existing.setPassword(null);
        }

        // 5. Call validate(), and add "Username already exists" if another staff member has the username
        List<String> errors = new ArrayList<>(existing.validate());

        if (existing.getUsername() != null && !existing.getUsername().trim().isEmpty()) {
            if (staffRepository.existsByUsernameAndUserIdNot(existing.getUsername(), userId)) {
                errors.add("Username already exists");
            }
        }

        // 6. Throw ValidationException if there are errors, else save(). userId and NIC never change.
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Staff saved = staffRepository.save(existing);

        // Write log
        writeLog("UPDATE", "Updated staff " + saved.getUserId() + " " + saved.getName());

        return saved;
    }

    /**
     * Delete staff member.
     */
    public void delete(String userId) {
        // 1. Load the record with getById(userId). A wrong ID throws NotFoundException.
        Staff staff = getById(userId);

        // Prevent deleting the only active administrator
        if (staff.getStaffType() == StaffType.ADMIN) {
            long activeAdminCount = staffRepository.findAll().stream()
                    .filter(s -> s.getStaffType() == StaffType.ADMIN && s.getStatus() == Status.ACTIVE)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ValidationException("Cannot delete the only active Administrator");
            }
        }

        // 2. Call delete(staff) to remove the record
        staffRepository.delete(staff);

        // Write log
        writeLog("DELETE", "Deleted staff " + staff.getUserId() + " " + staff.getName());
    }

    /**
     * Staff Login verification (Staff only can login).
     */
    public Staff login(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.isEmpty()) {
            throw new ValidationException("Invalid username or password");
        }

        // 1. Find the staff member with findByUsername(username)
        Optional<Staff> optionalStaff = staffRepository.findByUsername(username);

        if (optionalStaff.isEmpty()) {
            throw new ValidationException("Invalid username or password");
        }

        Staff staff = optionalStaff.get();

        // 2. Check the typed password equals the saved password
        if (!password.equals(staff.getPassword())) {
            throw new ValidationException("Invalid username or password");
        }

        // 3. Check canLogin(): staffType is ADMIN or RECEPTIONIST and status is ACTIVE
        if (!staff.canLogin()) {
            throw new ValidationException("Access denied. Only active staff members with portal access can sign in.");
        }

        // 4. On success, log in
        writeLog("LOGIN", staff.getUserId(), "Staff " + staff.getUserId() + " " + staff.getName() + " logged in");

        return staff;
    }

    /**
     * Staff Logout logging.
     */
    public void logout(Staff staff) {
        if (staff != null) {
            writeLog("LOGOUT", staff.getUserId(), "Staff " + staff.getUserId() + " " + staff.getName() + " logged out");
        }
    }

    /**
     * Returns history log lines newest first, read using BufferedReader.
     */
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
            System.err.println("Could not read staff log file: " + e.getMessage());
        }

        Collections.reverse(lines); // Newest first
        return lines;
    }

    private void writeLog(String action, String details) {
        String actorId = getCurrentActorId();
        writeLog(action, actorId, details);
    }

    /**
     * Writes log entry using BufferedWriter.
     */
    private void writeLog(String action, String actorId, String details) {
        try {
            File file = new File(logFilePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String logLine = timestamp + " | " + action + " | " + (actorId != null ? actorId : "SYSTEM") + " | " + details;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
                writer.write(logLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write to staff log file: " + e.getMessage());
        }
    }

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
