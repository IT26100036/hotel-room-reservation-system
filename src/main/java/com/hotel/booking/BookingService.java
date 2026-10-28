package com.hotel.booking;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.customer.CustomerService;
import com.hotel.room.Room;
import com.hotel.room.RoomService;
import com.hotel.room.RoomStatus;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Booking CRUD plus the stay flow: confirm, check in, check out, cancel.
 * Uses Customer (customer must exist) and Room (room must fit, room status changes).
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerService customerService;
    private final RoomService roomService;
    private final String logFilePath;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public BookingService(BookingRepository bookingRepository,
                          CustomerService customerService,
                          RoomService roomService,
                          @Value("${app.data.booking-log-file:data/booking_log.txt}") String logFilePath) {
        this.bookingRepository = bookingRepository;
        this.customerService = customerService;
        this.roomService = roomService;
        this.logFilePath = logFilePath;
    }

    /** Next ID: finds the highest number and adds 1 -> B001, B002... */
    public synchronized String generateNextId() {
        int max = 0;
        for (Booking booking : bookingRepository.findAll()) {
            String id = booking.getBookingId();
            if (id != null && id.toUpperCase().startsWith("B")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("B%03d", max + 1);
    }

    /* ---------------- Create ---------------- */

    /** Creates a walk-in or reservation booking (the form decided which subclass) */
    public synchronized Booking create(Booking booking) {
        // 1. Values the system sets
        booking.setBookingId(generateNextId());
        booking.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        booking.setStatus(booking.getStartingStatus());   // WALK_IN -> CONFIRMED, RESERVATION -> PENDING

        // 2. Rules of the booking and of its type
        List<String> errors = new ArrayList<>(booking.validate());

        // 3. Rules that need other parts of the system
        checkCustomer(booking, errors);
        checkRoom(booking, errors);
        checkRoomIsFree(booking, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        booking.setCustomerId(booking.getCustomerId().trim().toUpperCase());
        booking.setRoomId(booking.getRoomId().trim().toUpperCase());
        Booking saved = bookingRepository.save(booking);
        writeLog("CREATE", "Created " + saved.getBookingType() + " booking " + saved.getBookingId()
                + " for " + saved.getCustomerId() + " in room " + saved.getRoomId()
                + " (" + saved.getCheckInDate() + " to " + saved.getCheckOutDate() + ")");
        return saved;
    }

    /* ---------------- Read ---------------- */

    public List<Booking> getAll() {
        refreshStatuses();
        return bookingRepository.findAll();
    }

    public Booking getById(String bookingId) {
        refreshStatuses();
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    /** Empty keyword -> all bookings, else search by booking, customer or room ID */
    public List<Booking> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        refreshStatuses();
        return bookingRepository.search(keyword);
    }

    public List<Booking> getByStatus(BookingStatus status) {
        refreshStatuses();
        return bookingRepository.findByStatus(status);
    }

    /** Room price x nights, using the room's own calculatePrice() (suites add their service charge) */
    public double getRoomTotal(Booking booking) {
        try {
            Room room = roomService.getById(booking.getRoomId());
            return room.calculatePrice((int) booking.getNights());
        } catch (NotFoundException e) {
            return 0;
        }
    }

    /** Advance needed to confirm this booking (0 for walk-ins) */
    public double getRequiredAdvance(Booking booking) {
        return booking.getRequiredAdvance(getRoomTotal(booking));
    }

    /* ---------------- Update ---------------- */

    /** Changes room, dates or guests. Booking ID, type and customer never change. */
    public synchronized Booking update(String bookingId, Booking form) {
        Booking existing = getById(bookingId);

        if (!existing.isEditable()) {
            throw new ValidationException("Only pending or confirmed bookings can be changed");
        }

        // Copy only the editable fields
        existing.setRoomId(form.getRoomId() != null ? form.getRoomId().trim().toUpperCase() : null);
        existing.setCheckInDate(form.getCheckInDate());
        existing.setCheckOutDate(form.getCheckOutDate());
        existing.setNumberOfGuests(form.getNumberOfGuests());

        List<String> errors = new ArrayList<>(existing.validate());
        checkRoom(existing, errors);
        checkRoomIsFree(existing, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Booking saved = bookingRepository.save(existing);
        writeLog("UPDATE", "Updated booking " + saved.getBookingId() + " (room " + saved.getRoomId()
                + ", " + saved.getCheckInDate() + " to " + saved.getCheckOutDate() + ")");
        return saved;
    }

    /** PENDING -> CONFIRMED, after the advance is paid (Finance will call this later) */
    public Booking confirm(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ValidationException("Only a pending booking can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        writeLog("CONFIRM", "Confirmed booking " + booking.getBookingId());
        return booking;
    }

    /** CONFIRMED -> CHECKED_IN, room becomes OCCUPIED */
    public Booking checkIn(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only a confirmed booking can be checked in");
        }
        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Check-in is allowed from " + booking.getCheckInDate());
        }

        Room room = roomService.getById(booking.getRoomId());
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ValidationException("Room " + room.getRoomNumber() + " is not ready (" + room.getStatus() + ")");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);
        roomService.changeStatus(room.getRoomId(), RoomStatus.OCCUPIED);
        writeLog("CHECK_IN", "Checked in booking " + booking.getBookingId() + " to room " + room.getRoomNumber());
        return booking;
    }

    /** CHECKED_IN -> CHECKED_OUT, room becomes CLEANING */
    public Booking checkOut(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ValidationException("Only a checked-in booking can be checked out");
        }
        booking.setStatus(BookingStatus.CHECKED_OUT);
        bookingRepository.save(booking);
        roomService.changeStatus(booking.getRoomId(), RoomStatus.CLEANING);
        writeLog("CHECK_OUT", "Checked out booking " + booking.getBookingId());
        return booking;
    }

    /** PENDING or CONFIRMED -> CANCELLED */
    public Booking cancel(String bookingId) {
        Booking booking = getById(bookingId);
        if (!booking.isEditable()) {
            throw new ValidationException("Only a pending or confirmed booking can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        writeLog("CANCEL", "Cancelled booking " + booking.getBookingId());
        return booking;
    }

    /* ---------------- Delete ---------------- */

    /** Only cancelled or no-show bookings can be removed */
    public void delete(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.NO_SHOW) {
            throw new ValidationException("Only cancelled or no-show bookings can be deleted");
        }
        bookingRepository.delete(booking);
        writeLog("DELETE", "Deleted booking " + booking.getBookingId());
    }

    /* ---------------- Rules that use other parts ---------------- */

    private void checkCustomer(Booking booking, List<String> errors) {
        if (booking.getCustomerId() == null || booking.getCustomerId().trim().isEmpty()) {
            return;   // "Customer is required" already added by validate()
        }
        try {
            customerService.getById(booking.getCustomerId().trim());
        } catch (NotFoundException e) {
            errors.add("Customer not found");
        }
    }

    private void checkRoom(Booking booking, List<String> errors) {
        if (booking.getRoomId() == null || booking.getRoomId().trim().isEmpty()) {
            return;   // "Room is required" already added by validate()
        }
        try {
            Room room = roomService.getById(booking.getRoomId().trim());
            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                errors.add("Room " + room.getRoomNumber() + " is under maintenance");
            }
            if (booking.getNumberOfGuests() > room.getCapacity()) {
                errors.add("Room " + room.getRoomNumber() + " fits only " + room.getCapacity() + " guest(s)");
            }
        } catch (NotFoundException e) {
            errors.add("Room not found");
        }
    }

    /** The room must not have another active booking on the same nights */
    private void checkRoomIsFree(Booking booking, List<String> errors) {
        if (booking.getRoomId() == null || booking.getCheckInDate() == null || booking.getCheckOutDate() == null) {
            return;
        }
        for (Booking other : bookingRepository.findActiveByRoomId(booking.getRoomId().trim())) {
            if (other.getBookingId().equalsIgnoreCase(booking.getBookingId())) {
                continue;   // the booking being updated
            }
            if (other.overlaps(booking.getCheckInDate(), booking.getCheckOutDate())) {
                errors.add("Room is already booked from " + other.getCheckInDate() + " to "
                        + other.getCheckOutDate() + " (" + other.getBookingId() + ")");
                return;
            }
        }
    }

    /**
     * Daily check, run whenever bookings are read:
     * PENDING older than 24 hours -> CANCELLED, CONFIRMED with a past check-in date -> NO_SHOW
     */
    private synchronized void refreshStatuses() {
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.isExpired()) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                writeLog("EXPIRE", "Booking " + booking.getBookingId() + " cancelled: advance not paid within 24 hours");
            } else if (booking.isNoShow()) {
                booking.setStatus(BookingStatus.NO_SHOW);
                bookingRepository.save(booking);
                writeLog("NO_SHOW", "Booking " + booking.getBookingId() + " marked NO_SHOW: guest did not arrive");
            }
        }
    }

    /* ---------------- History log ---------------- */

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
            System.err.println("Could not read booking log: " + e.getMessage());
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
            System.err.println("Could not write booking log: " + e.getMessage());
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
