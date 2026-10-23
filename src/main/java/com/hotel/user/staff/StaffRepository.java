package com.hotel.user.staff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File-based repository for Staff data using BufferedReader and BufferedWriter.
 * Persists staff members in a pipe-delimited text file (e.g. data/staff.txt).
 */
@Repository
public class StaffRepository {

    private final String filePath;
    private static final String HEADER = "#userId|name|nicOrPassport|contactNo|email|address|staffType|position|salary|joinedDate|username|password|status";

    public StaffRepository(@Value("${app.data.staff-file:data/staff.txt}") String filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    private synchronized void ensureFileExists() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
                    writer.write(HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not initialize staff data file: " + e.getMessage());
        }
    }

    /**
     * Reads all staff members from the text file using BufferedReader.
     */
    public synchronized List<Staff> findAll() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<Staff> staffList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Staff staff = parseLine(line);
                if (staff != null) {
                    staffList.add(staff);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading staff file using BufferedReader: " + e.getMessage());
        }
        return staffList;
    }

    /**
     * Finds a staff member by userId.
     */
    public Optional<Staff> findById(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(s -> userId.equalsIgnoreCase(s.getUserId()))
                .findFirst();
    }

    /**
     * Saves a staff member (creates new or updates existing) using BufferedWriter.
     */
    public synchronized Staff save(Staff staff) {
        List<Staff> allStaff = findAll();
        boolean updated = false;

        for (int i = 0; i < allStaff.size(); i++) {
            if (allStaff.get(i).getUserId() != null && allStaff.get(i).getUserId().equalsIgnoreCase(staff.getUserId())) {
                allStaff.set(i, staff);
                updated = true;
                break;
            }
        }

        if (!updated) {
            allStaff.add(staff);
        }

        writeAll(allStaff);
        return staff;
    }

    /**
     * Deletes a staff member from the text file.
     */
    public synchronized void delete(Staff staff) {
        if (staff == null || staff.getUserId() == null) {
            return;
        }
        deleteById(staff.getUserId());
    }

    public synchronized void deleteById(String userId) {
        List<Staff> allStaff = findAll();
        List<Staff> filtered = allStaff.stream()
                .filter(s -> !userId.equalsIgnoreCase(s.getUserId()))
                .collect(Collectors.toList());
        writeAll(filtered);
    }

    public synchronized void flush() {
        // Text file writes are already flushed immediately upon writeAll
    }

    /**
     * Counts staff members matching given NIC.
     */
    public long countByNic(String nic) {
        if (nic == null) return 0;
        return findAll().stream()
                .filter(s -> nic.equalsIgnoreCase(s.getNicOrPassport()))
                .count();
    }

    /**
     * Checks if a username already exists.
     */
    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return findAll().stream()
                .anyMatch(s -> s.getUsername() != null && s.getUsername().equalsIgnoreCase(username.trim()));
    }

    /**
     * Finds a staff member by username.
     */
    public Optional<Staff> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        return findAll().stream()
                .filter(s -> s.getUsername() != null && s.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    /**
     * Checks if a username is taken by any other staff member.
     */
    public boolean existsByUsernameAndUserIdNot(String username, String userId) {
        if (username == null || username.trim().isEmpty()) return false;
        return findAll().stream()
                .anyMatch(s -> s.getUsername() != null
                        && s.getUsername().equalsIgnoreCase(username.trim())
                        && !s.getUserId().equalsIgnoreCase(userId));
    }

    /**
     * Search staff members by name or NIC or userId (case-insensitive).
     */
    public List<Staff> search(String keyword) {
        List<Staff> allStaff = findAll();
        if (keyword == null || keyword.trim().isEmpty()) {
            return allStaff;
        }
        String lowerKeyword = keyword.trim().toLowerCase();
        return allStaff.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(lowerKeyword))
                        || (s.getNicOrPassport() != null && s.getNicOrPassport().toLowerCase().contains(lowerKeyword))
                        || (s.getUserId() != null && s.getUserId().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * Writes all staff members to file using BufferedWriter.
     */
    private synchronized void writeAll(List<Staff> staffList) {
        ensureFileExists();
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Staff staff : staffList) {
                writer.write(formatStaff(staff));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing staff file using BufferedWriter: " + e.getMessage());
        }
    }

    private String formatStaff(Staff s) {
        return String.join("|",
                escape(s.getUserId()),
                escape(s.getName()),
                escape(s.getNicOrPassport()),
                escape(s.getContactNo()),
                escape(s.getEmail()),
                escape(s.getAddress()),
                s.getStaffType() != null ? s.getStaffType().name() : "",
                escape(s.getPosition()),
                String.valueOf(s.getSalary()),
                s.getJoinedDate() != null ? s.getJoinedDate().toString() : "",
                escape(s.getUsername()),
                escape(s.getPassword()),
                s.getStatus() != null ? s.getStatus().name() : ""
        );
    }

    private String escape(String value) {
        if (value == null) return "";
        // Replace newlines or pipe characters if any to avoid corrupted format
        return value.replace("\r", "").replace("\n", " ").replace("|", "-").trim();
    }

    private Staff parseLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 13) {
                return null;
            }
            Staff staff = new Staff();
            staff.setUserId(parts[0].trim());
            staff.setName(parts[1].trim());
            staff.setNicOrPassport(parts[2].trim());
            staff.setContactNo(parts[3].trim());
            staff.setEmail(parts[4].trim().isEmpty() ? null : parts[4].trim());
            staff.setAddress(parts[5].trim().isEmpty() ? null : parts[5].trim());

            if (!parts[6].trim().isEmpty()) {
                staff.setStaffType(StaffType.valueOf(parts[6].trim()));
            }
            staff.setPosition(parts[7].trim());
            if (!parts[8].trim().isEmpty()) {
                staff.setSalary(Double.parseDouble(parts[8].trim()));
            }
            if (!parts[9].trim().isEmpty()) {
                staff.setJoinedDate(LocalDate.parse(parts[9].trim()));
            }
            staff.setUsername(parts[10].trim().isEmpty() ? null : parts[10].trim());
            staff.setPassword(parts[11].trim().isEmpty() ? null : parts[11].trim());
            if (!parts[12].trim().isEmpty()) {
                staff.setStatus(Status.valueOf(parts[12].trim()));
            }
            return staff;
        } catch (Exception e) {
            System.err.println("Could not parse staff line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}
