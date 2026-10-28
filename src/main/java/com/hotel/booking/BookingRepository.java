package com.hotel.booking;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saves bookings in a text file (data/bookings.txt) using BufferedReader and BufferedWriter.
 * One booking per line, fields separated by |
 * The bookingType column decides whether a line becomes a WalkInBooking or a ReservationBooking.
 */
@Repository
public class BookingRepository {

    private final String filePath;
    private static final String HEADER =
            "#bookingId|bookingType|customerId|roomId|checkInDate|checkOutDate|numberOfGuests|status|createdAt";

    public BookingRepository(@Value("${app.data.booking-file:data/bookings.txt}") String filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    /** Creates the data folder and the file with its header line if they are missing */
    private synchronized void ensureFileExists() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    writer.write(HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not create booking file: " + e.getMessage());
        }
    }

    /** Reads every booking from the file */
    public synchronized List<Booking> findAll() {
        List<Booking> bookings = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return bookings;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;   // skip empty lines and the header
                }
                Booking booking = parseLine(line);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read booking file: " + e.getMessage());
        }
        return bookings;
    }

    public Optional<Booking> findById(String bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(b -> bookingId.trim().equalsIgnoreCase(b.getBookingId()))
                .findFirst();
    }

    /** Bookings whose booking ID, customer ID or room ID contains the keyword */
    public List<Booking> search(String keyword) {
        String k = keyword.trim().toLowerCase();
        return findAll().stream()
                .filter(b -> contains(b.getBookingId(), k)
                        || contains(b.getCustomerId(), k)
                        || contains(b.getRoomId(), k))
                .collect(Collectors.toList());
    }

    public List<Booking> findByStatus(BookingStatus status) {
        return findAll().stream()
                .filter(b -> b.getStatus() == status)
                .collect(Collectors.toList());
    }

    /** Active bookings (PENDING, CONFIRMED, CHECKED_IN) of one room */
    public List<Booking> findActiveByRoomId(String roomId) {
        return findAll().stream()
                .filter(b -> b.getRoomId().equalsIgnoreCase(roomId))
                .filter(Booking::isActive)
                .collect(Collectors.toList());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    /** Adds a new booking, or replaces the one with the same ID */
    public synchronized Booking save(Booking booking) {
        List<Booking> all = findAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getBookingId().equalsIgnoreCase(booking.getBookingId())) {
                all.set(i, booking);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(booking);
        }

        writeAll(all);
        return booking;
    }

    public synchronized void delete(Booking booking) {
        List<Booking> remaining = findAll().stream()
                .filter(b -> !b.getBookingId().equalsIgnoreCase(booking.getBookingId()))
                .collect(Collectors.toList());
        writeAll(remaining);
    }

    /** Rewrites the whole file */
    private synchronized void writeAll(List<Booking> bookings) {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Booking b : bookings) {
                writer.write(formatBooking(b));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write booking file: " + e.getMessage());
        }
    }

    /** Booking -> one line of text */
    private String formatBooking(Booking b) {
        return String.join("|",
                b.getBookingId(),
                b.getBookingType().name(),
                b.getCustomerId(),
                b.getRoomId(),
                b.getCheckInDate().toString(),
                b.getCheckOutDate().toString(),
                String.valueOf(b.getNumberOfGuests()),
                b.getStatus().name(),
                b.getCreatedAt() != null ? b.getCreatedAt().toString() : ""
        );
    }

    /** One line of text -> WalkInBooking or ReservationBooking */
    private Booking parseLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 9) {
                return null;
            }

            String bookingId = p[0].trim();
            BookingType type = BookingType.valueOf(p[1].trim());
            String customerId = p[2].trim();
            String roomId = p[3].trim();
            LocalDate checkIn = LocalDate.parse(p[4].trim());
            LocalDate checkOut = LocalDate.parse(p[5].trim());
            int guests = Integer.parseInt(p[6].trim());
            BookingStatus status = BookingStatus.valueOf(p[7].trim());
            LocalDateTime createdAt = p[8].trim().isEmpty() ? null : LocalDateTime.parse(p[8].trim());

            if (type == BookingType.RESERVATION) {
                return new ReservationBooking(bookingId, customerId, roomId, checkIn, checkOut, guests, status, createdAt);
            }
            return new WalkInBooking(bookingId, customerId, roomId, checkIn, checkOut, guests, status, createdAt);
        } catch (Exception e) {
            System.err.println("Could not read booking line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}
