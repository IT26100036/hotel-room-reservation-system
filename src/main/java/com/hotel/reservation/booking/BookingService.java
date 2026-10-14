package com.hotel.reservation.booking;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// Business rules for bookings.
// Controllers call this class; only BookingDao talks to the database.

@Service
public class BookingService {
    private final BookingDao bookingDao;

    public BookingService(BookingDao bookingDao) {
        this.bookingDao = bookingDao;
    }

    // Returns all bookings, newest first
    public List<Booking> getAllBookings() {
        return bookingDao.findAll();
    }

    // Returns only the bookings with this status, newest first
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingDao.findByStatus(status);
    }

    // Returns the booking with this id, or throws if it does not exist
    public Booking getBooking(int id) {
        Booking booking = bookingDao.findById(id);
        if (booking == null)
            throw new IllegalArgumentException("Booking not found: " + id);
        return booking;
    }

    // Creates a confirmed walk-in booking after checking the room is free.
    public int createWalkIn(int guestId, int roomId, LocalDate from, LocalDate to) {
        checkDates(from, to);
        WalkInBooking booking = new WalkInBooking(0, guestId, roomId, from, to, BookingStatus.CONFIRMED);
        if (bookingDao.hasOverlap(roomId, from, to, 0))
            throw new IllegalStateException("Room is already booked for these dates");
        return bookingDao.create(booking);
    }

    // Changes the room and dates of a confirmed booking
    public void updateBooking(int id, int roomId, LocalDate from, LocalDate to) {
        Booking booking = getBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Only a confirmed booking can be changed");
        checkDates(from, to);
        if (bookingDao.hasOverlap(roomId, from, to, id))
            throw new IllegalStateException("Room is already booked for these dates");
        booking.setRoomId(roomId);
        booking.setDates(from, to);
        bookingDao.update(booking);
    }

    // Both dates must be given, and a booking cannot start in the past
    private void checkDates(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("Both dates are required");
        if (from.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Check-in date cannot be in the past");
    }
}
