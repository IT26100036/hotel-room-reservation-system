package com.hotel.booking;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A booking of one room by one customer.
 * Abstract: every booking is a walk-in or a reservation (see the subclasses).
 */
public abstract class Booking {

    /** A PENDING reservation is cancelled if the advance is not paid within this time */
    public static final int PENDING_EXPIRY_HOURS = 24;

    /** Attributes */
    private String bookingId;        // B001, B002...
    private String customerId;       // C001...
    private String roomId;           // R001...

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    private int numberOfGuests;
    private BookingStatus status;
    private LocalDateTime createdAt;

    /** Constructors */
    public Booking() {
    }

    public Booking(String bookingId, String customerId, String roomId, LocalDate checkInDate,
                   LocalDate checkOutDate, int numberOfGuests, BookingStatus status, LocalDateTime createdAt) {
        setBookingId(bookingId);
        setCustomerId(customerId);
        setRoomId(roomId);
        setCheckInDate(checkInDate);
        setCheckOutDate(checkOutDate);
        setNumberOfGuests(numberOfGuests);
        setStatus(status);
        setCreatedAt(createdAt);
    }

    /** Abstract methods */
    // WALK_IN or RESERVATION
    public abstract BookingType getBookingType();

    // Status a new booking of this type starts with
    public abstract BookingStatus getStartingStatus();

    // Part of the room total that must be paid in advance
    public abstract double getRequiredAdvance(double roomTotal);

    // Extra rules that only one booking type has
    protected abstract void validateTypeRules(List<String> errors);

    /** Number of nights between check-in and check-out */
    public long getNights() {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /** PENDING, CONFIRMED and CHECKED_IN bookings hold the room */
    public boolean isActive() {
        return status == BookingStatus.PENDING
                || status == BookingStatus.CONFIRMED
                || status == BookingStatus.CHECKED_IN;
    }

    /** Only PENDING and CONFIRMED bookings can be changed */
    public boolean isEditable() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    /** A PENDING booking older than 24 hours has expired */
    public boolean isExpired() {
        return status == BookingStatus.PENDING
                && createdAt != null
                && createdAt.plusHours(PENDING_EXPIRY_HOURS).isBefore(LocalDateTime.now());
    }

    /** A CONFIRMED booking whose check-in date has passed without check-in */
    public boolean isNoShow() {
        return status == BookingStatus.CONFIRMED
                && checkInDate != null
                && checkInDate.isBefore(LocalDate.now());
    }

    /** True if the two stays share at least one night */
    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        if (checkInDate == null || checkOutDate == null || otherCheckIn == null || otherCheckOut == null) {
            return false;
        }
        return checkInDate.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOutDate);
    }

    /** Checks all rules and returns every error found */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer is required");
        }
        if (roomId == null || roomId.trim().isEmpty()) {
            errors.add("Room is required");
        }
        if (checkInDate == null) {
            errors.add("Check-in date is required");
        }
        if (checkOutDate == null) {
            errors.add("Check-out date is required");
        }
        if (checkInDate != null && checkOutDate != null && !checkOutDate.isAfter(checkInDate)) {
            errors.add("Check-out date must be after the check-in date");
        }
        if (numberOfGuests < 1) {
            errors.add("Number of guests must be at least 1");
        }

        // Rules of the subclass (walk-in or reservation)
        validateTypeRules(errors);
        return errors;
    }

    /** Getters and Setters */
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
