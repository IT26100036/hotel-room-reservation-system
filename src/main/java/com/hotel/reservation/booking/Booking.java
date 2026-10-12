package com.hotel.reservation.booking;

import com.hotel.reservation.common.RoomAllocation;
import java.time.LocalDate;

public abstract class Booking extends RoomAllocation {
    private BookingStatus status;

    protected Booking() {
    }

    protected Booking(int id, int guestId, int roomId, LocalDate dateFrom, LocalDate dateTo, BookingStatus status) {
        super(id, guestId, roomId, dateFrom, dateTo);
        setStatus(status);
    }

    public BookingStatus getStatus() {
        return status;
    }

    // ---------- status changes: the only way to move a booking forward ----------
    // CONFIRMED -> CHECKED_IN
    public void checkIn() {
        if (status != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Only a confirmed booking can be checked in. Current status: " + status);
        setStatus(BookingStatus.CHECKED_IN);
    }

    // CHECKED_IN -> CHECKED_OUT
    public void checkOut() {
        if (status != BookingStatus.CHECKED_IN)
            throw new IllegalStateException("Only a checked-in booking can be checked out. Current status: " + status);
        setStatus(BookingStatus.CHECKED_OUT);
    }

    // CONFIRMED -> CANCELLED
    public void cancel() {
        if (status != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Only a confirmed booking can be cancelled. Current status: " + status);
        setStatus(BookingStatus.CANCELLED);
    }

    protected void setStatus(BookingStatus status) {
        if (status == null)
            throw new IllegalArgumentException("Booking status is required");
        this.status = status;
    }

    @Override
    public String getDisplayName() {
        return "B-" + getId();
    }

    @Override
    public String getAllocationType() {
        return "BOOKING";
    }

    // Amount the guest still has to pay for the room
    public abstract double calculateAmountDue(double roomTotal);

    // "RESERVATION" or "WALK_IN", saved in the booking_type column
    public abstract String getBookingType();
}
