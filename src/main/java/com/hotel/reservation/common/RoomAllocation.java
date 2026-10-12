package com.hotel.reservation.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public abstract class RoomAllocation extends BaseEntity{
    private int guestId;
    private int roomId;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    protected RoomAllocation() {
    }

    protected RoomAllocation(int id, int guestId, int roomId, LocalDate dateFrom, LocalDate dateTo) {
        super(id);
        setGuestId(guestId);
        setRoomId(roomId);
        setDates(dateFrom, dateTo);
    }

    public int getGuestId() {
        return guestId;
    }

    public void setGuestId(int guestId) {
        if (guestId <= 0)
            throw new IllegalArgumentException("A valid guest is required");
        this.guestId = guestId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        if (roomId <= 0)
            throw new IllegalArgumentException("A valid room is required");
        this.roomId = roomId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDates(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null)
            throw new IllegalArgumentException("Both dates are required");
        if (!dateTo.isAfter(dateFrom))
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    // Number of nights between check-in and check-out
    public long getNights() {
        return ChronoUnit.DAYS.between(dateFrom, dateTo);
    }

    // True if this allocation shares at least one night with the given range
    public boolean overlaps(LocalDate from, LocalDate to) {
        return from.isBefore(dateTo) && to.isAfter(dateFrom);
    }

    // "RESERVATION" or "BOOKING", defined by each subclass
    public abstract String getAllocationType();
}
