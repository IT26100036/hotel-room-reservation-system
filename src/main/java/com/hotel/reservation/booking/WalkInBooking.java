package com.hotel.reservation.booking;

import java.time.LocalDate;

// A booking for a guest who arrives without a reservation
// No advance was paid, so the full room total is due

public class WalkInBooking extends Booking {

    public WalkInBooking() {
    }

    public WalkInBooking(int id, int guestId, int roomId, LocalDate dateFrom, LocalDate dateTo, BookingStatus status) {
        super(id, guestId, roomId, dateFrom, dateTo, status);
    }

    // Walk-in guests pay the full room total
    @Override
    public double calculateAmountDue(double roomTotal) {
        return roomTotal;
    }

    // Saved in the booking_type column
    @Override
    public String getBookingType() {
        return "WALK_IN";
    }
}
