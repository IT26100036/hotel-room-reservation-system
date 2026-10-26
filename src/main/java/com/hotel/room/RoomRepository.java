package com.hotel.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File-based repository for Room data using BufferedReader and BufferedWriter.
 * Persists rooms in a pipe-delimited text file (e.g. data/rooms.txt).
 * The roomType column decides whether a line becomes a SingleRoom, DoubleRoom or SuiteRoom.
 */
@Repository
public class RoomRepository {

    private final String filePath;
    private static final String HEADER = "#roomId|roomType|roomNumber|floor|capacity|pricePerNight|hasAirConditioning|status|description";

    public RoomRepository(@Value("${app.data.room-file:data/rooms.txt}") String filePath) {
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
            System.err.println("Could not initialize room data file: " + e.getMessage());
        }
    }

    /**
     * Reads all rooms from the text file using BufferedReader.
     */
    public synchronized List<Room> findAll() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<Room> rooms = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Room room = parseLine(line);
                if (room != null) {
                    rooms.add(room);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading room file using BufferedReader: " + e.getMessage());
        }
        return rooms;
    }

    /**
     * Finds a room by roomId.
     */
    public Optional<Room> findById(String roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(r -> roomId.equalsIgnoreCase(r.getRoomId()))
                .findFirst();
    }

    /**
     * Saves a room (creates new or updates existing) using BufferedWriter.
     */
    public synchronized Room save(Room room) {
        List<Room> all = findAll();
        boolean updated = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRoomId() != null && all.get(i).getRoomId().equalsIgnoreCase(room.getRoomId())) {
                all.set(i, room);
                updated = true;
                break;
            }
        }

        if (!updated) {
            all.add(room);
        }

        writeAll(all);
        return room;
    }

    /**
     * Deletes a room from the text file.
     */
    public synchronized void delete(Room room) {
        if (room == null || room.getRoomId() == null) {
            return;
        }
        String id = room.getRoomId();
        List<Room> filtered = findAll().stream()
                .filter(r -> !id.equalsIgnoreCase(r.getRoomId()))
                .collect(Collectors.toList());
        writeAll(filtered);
    }

    /**
     * Checks if a room number is already used.
     */
    public boolean existsByRoomNumber(String roomNumber) {
        if (roomNumber == null) return false;
        return findAll().stream()
                .anyMatch(r -> roomNumber.trim().equals(r.getRoomNumber()));
    }

    /**
     * Rooms whose number contains the given text.
     */
    public List<Room> findByRoomNumberContaining(String roomNumber) {
        return findAll().stream()
                .filter(r -> r.getRoomNumber() != null && r.getRoomNumber().contains(roomNumber.trim()))
                .collect(Collectors.toList());
    }

    /**
     * Rooms with the given status.
     */
    public List<Room> findByStatus(RoomStatus status) {
        return findAll().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Writes all rooms to file using BufferedWriter.
     */
    private synchronized void writeAll(List<Room> rooms) {
        ensureFileExists();
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Room r : rooms) {
                writer.write(formatRoom(r));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing room file using BufferedWriter: " + e.getMessage());
        }
    }

    private String formatRoom(Room r) {
        return String.join("|",
                escape(r.getRoomId()),
                r.getRoomType().name(),
                escape(r.getRoomNumber()),
                String.valueOf(r.getFloor()),
                String.valueOf(r.getCapacity()),
                String.valueOf(r.getPricePerNight()),
                String.valueOf(r.isHasAirConditioning()),
                r.getStatus() != null ? r.getStatus().name() : "",
                escape(r.getDescription())
        );
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\r", "").replace("\n", " ").replace("|", "-").trim();
    }

    /** Creates the right subclass for the room type. */
    private Room newRoom(RoomType type) {
        switch (type) {
            case DOUBLE:
                return new DoubleRoom();
            case SUITE:
                return new SuiteRoom();
            default:
                return new SingleRoom();
        }
    }

    private Room parseLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 9) {
                return null;
            }

            // The type column decides which subclass to create
            Room room = newRoom(RoomType.valueOf(parts[1].trim()));

            room.setRoomId(parts[0].trim());
            room.setRoomNumber(parts[2].trim());
            room.setFloor(Integer.parseInt(parts[3].trim()));
            room.setCapacity(Integer.parseInt(parts[4].trim()));
            room.setPricePerNight(Double.parseDouble(parts[5].trim()));
            room.setHasAirConditioning(Boolean.parseBoolean(parts[6].trim()));
            if (!parts[7].trim().isEmpty()) {
                room.setStatus(RoomStatus.valueOf(parts[7].trim()));
            }
            room.setDescription(parts[8].trim().isEmpty() ? null : parts[8].trim());
            return room;
        } catch (Exception e) {
            System.err.println("Could not parse room line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}
