package com.hotel.room;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Room CRUD. ADMIN manages rooms; RECEPTIONIST can view them.
 * Booking Management uses changeStatus() and calculatePrice().
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final String logFilePath;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public RoomService(RoomRepository roomRepository,
                       @Value("${app.data.room-log-file:data/room_log.txt}") String logFilePath) {
        this.roomRepository = roomRepository;
        this.logFilePath = logFilePath;
    }

    /**
     * Generate the next ID: read all room IDs, take the highest number, add 1, format as R001, R002...
     */
    public synchronized String generateNextId() {
        int maxId = 0;
        for (Room r : roomRepository.findAll()) {
            String id = r.getRoomId();
            if (id != null && id.toUpperCase().startsWith("R")) {
                try {
                    maxId = Math.max(maxId, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("R%03d", maxId + 1);
    }

    /**
     * Create a room. The form's room type decided which subclass was made.
     */
    public Room create(Room room) {
        // 1. Generate next ID: R001, R002...
        room.setRoomId(generateNextId());

        // 2. Set status to AVAILABLE
        room.setStatus(RoomStatus.AVAILABLE);

        // 3. Call validate(). The capacity check uses getMaxCapacity(), so each subclass gives its own limit
        List<String> errors = new ArrayList<>(room.validate());

        // 4. Add "Room number already exists" if existsByRoomNumber is true
        if (room.getRoomNumber() != null && roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            errors.add("Room number already exists");
        }

        // 5. If the list has errors, throw ValidationException with all of them
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 6. Otherwise save
        room.setRoomNumber(room.getRoomNumber().trim());
        Room saved = roomRepository.save(room);
        writeLog("CREATE", "Added " + saved.getRoomType() + " room " + saved.getRoomId() + " (" + saved.getRoomNumber() + ")");
        return saved;
    }

    /**
     * getAll() returns every room.
     */
    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    /**
     * getById() returns the room, or throws NotFoundException ("Room not found").
     */
    public Room getById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found"));
    }

    /**
     * search() returns every room for an empty room number, else rooms whose number contains it.
     */
    public List<Room> search(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return getAll();
        }
        return roomRepository.findByRoomNumberContaining(roomNumber);
    }

    /**
     * getByStatus() returns rooms with that status right now.
     */
    public List<Room> getByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status);
    }

    /**
     * getByType() keeps the rooms whose getRoomType() matches.
     */
    public List<Room> getByType(RoomType type) {
        return getAll().stream()
                .filter(r -> r.getRoomType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing room. roomId, roomNumber and roomType never change.
     */
    public Room update(String roomId, Room form) {
        // 1. Load the existing record
        Room existing = getById(roomId);

        // 2. Copy the editable fields from the form
        existing.setFloor(form.getFloor());
        existing.setCapacity(form.getCapacity());
        existing.setPricePerNight(form.getPricePerNight());
        existing.setHasAirConditioning(form.isHasAirConditioning());
        existing.setStatus(form.getStatus());
        existing.setDescription(form.getDescription());

        // 3. Call validate()
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 4. Save
        Room saved = roomRepository.save(existing);
        writeLog("UPDATE", "Updated room " + saved.getRoomId() + " (" + saved.getRoomNumber() + ")");
        return saved;
    }

    /**
     * Change only the status. Booking Management calls this to mark a room
     * OCCUPIED at check-in and CLEANING at check-out.
     */
    public Room changeStatus(String roomId, RoomStatus status) {
        Room room = getById(roomId);
        room.setStatus(status);
        Room saved = roomRepository.save(room);
        writeLog("CHANGE_STATUS", "Room " + saved.getRoomId() + " (" + saved.getRoomNumber() + ") set to " + status);
        return saved;
    }

    /**
     * Delete a room.
     * When Booking Management is added, rooms with bookings must not be deleted.
     */
    public void delete(String roomId) {
        // 1. Load the record. A wrong ID throws NotFoundException.
        Room room = getById(roomId);

        // 2. Remove it from the file
        roomRepository.delete(room);
        writeLog("DELETE", "Deleted room " + room.getRoomId() + " (" + room.getRoomNumber() + ")");
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
            System.err.println("Could not read room log file: " + e.getMessage());
        }

        Collections.reverse(lines); // Newest first
        return lines;
    }

    /**
     * Writes one log line using BufferedWriter in append mode.
     */
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
            System.err.println("Could not write to room log file: " + e.getMessage());
        }
    }

    /** The logged-in staff member who made the change, or SYSTEM. */
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
