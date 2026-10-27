package com.hotel.finance.services;

import com.hotel.user.staff.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

/**
 * History log for finance (data/finance_log.txt).
 * Line format: time | action | staff ID | details
 */

@Component
public class FinanceLog {
    private final String logFilePath;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public FinanceLog(@Value("${app.data.finance-log-file:data/finance_log.txt}") String logFilePath) {
        this.logFilePath = logFilePath;
    }

    /** Adds one line to the end of the log (append mode) */
    public void write(String action, String details) {
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
            System.err.println("Could not write finance log: " + e.getMessage());
        }
    }

    /** All log lines, newest first */
    public List<String> readAll() {
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
            System.err.println("Could not read finance log: " + e.getMessage());
        }

        Collections.reverse(lines);   // newest first
        return lines;
    }

    /** ID of the logged-in staff member who made the change, or SYSTEM */
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