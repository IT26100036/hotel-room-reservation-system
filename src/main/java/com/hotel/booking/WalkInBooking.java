package com.hotel.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The guest is at the front desk now.
 * Starts CONFIRMED, check-in must be today, and no advance is needed.
 */
public class WalkInBooking extends Booking {

    public WalkInBooking() {
    }

    public WalkInBooking(String bookingId, String customerId, String roomId, LocalDate checkInDate,
                         LocalDate checkOutDate, int numberOfGuests, BookingStatus status, LocalDateTime createdAt) {
        super(bookingId, customerId, roomId, checkInDate, checkOutDate, numberOfGuests, status, createdAt);
    }

    @Override
    public BookingType getBookingType() {
        return BookingType.WALK_IN;
    }

    @Override
    public BookingStatus getStartingStatus() {
        return BookingStatus.CONFIRMED;
    }

    @Override
    public double getRequiredAdvance(double roomTotal) {
        return 0;   // a walk-in pays at the desk, no advance
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        if (getCheckInDate() != null && !getCheckInDate().equals(LocalDate.now())) {
            errors.add("A walk-in booking must check in today");
        }
    }
}
