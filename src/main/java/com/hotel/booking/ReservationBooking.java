package com.hotel.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booked in advance (phone, email, at the desk for a later date).
 * Starts PENDING until the advance is paid, then CONFIRMED.
 */
public class ReservationBooking extends Booking {

    /** 25% of the room total must be paid to confirm a reservation */
    public static final double ADVANCE_RATE = 0.25;

    public ReservationBooking() {
    }

    public ReservationBooking(String bookingId, String customerId, String roomId, LocalDate checkInDate,
                              LocalDate checkOutDate, int numberOfGuests, BookingStatus status, LocalDateTime createdAt) {
        super(bookingId, customerId, roomId, checkInDate, checkOutDate, numberOfGuests, status, createdAt);
    }

    @Override
    public BookingType getBookingType() {
        return BookingType.RESERVATION;
    }

    @Override
    public BookingStatus getStartingStatus() {
        return BookingStatus.PENDING;
    }

    @Override
    public double getRequiredAdvance(double roomTotal) {
        return roomTotal * ADVANCE_RATE;
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        if (getCheckInDate() != null && getCheckInDate().isBefore(LocalDate.now())) {
            errors.add("Check-in date cannot be in the past");
        }
    }
}
