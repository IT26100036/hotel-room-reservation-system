package com.hotel.reservation.booking;

import java.time.LocalDate;

// A booking created when staff convert a reservation (guest has arrived)
// The advance paid at reservation time is deducted from the room total

public class ReservationBooking extends Booking {
    private int reservationId;
    private double advancePaid;

    public ReservationBooking() {
    }

    public ReservationBooking(int id, int guestId, int roomId, LocalDate dateFrom, LocalDate dateTo,
            BookingStatus status, int reservationId, double advancePaid) {
        super(id, guestId, roomId, dateFrom, dateTo, status);
        setReservationId(reservationId);
        setAdvancePaid(advancePaid);
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        if (reservationId <= 0)
            throw new IllegalArgumentException("A valid reservation is required");
        this.reservationId = reservationId;
    }

    public double getAdvancePaid() {
        return advancePaid;
    }

    public void setAdvancePaid(double advancePaid) {
        if (advancePaid < 0.0)
            throw new IllegalArgumentException("Advance paid cannot be negative");
        this.advancePaid = advancePaid;
    }

    // Room total minus the advance, never below zero
    @Override
    public double calculateAmountDue(double roomTotal) {
        return Math.max(0, roomTotal - advancePaid);
    }

    // Saved in the booking_type column
    @Override
    public String getBookingType() {
        return "RESERVATION";
    }
}
