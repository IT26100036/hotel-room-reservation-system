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

    // Moves the check-out date later for a guest who is already in the room.
    // Only check-out changes; the room must be free for the extra nights.
    public void extendStay(int id, LocalDate newDateTo) {
        Booking booking = getBooking(id);
        if (booking.getStatus() != BookingStatus.CHECKED_IN)
            throw new IllegalStateException("Only a checked-in booking can be extended");
        if (newDateTo == null)
            throw new IllegalArgumentException("New check-out date is required");
        if (!newDateTo.isAfter(booking.getDateTo()))
            throw new IllegalArgumentException("New check-out date must be after " + booking.getDateTo());
        // Only the extra nights need to be free
        if (bookingDao.hasOverlap(booking.getRoomId(), booking.getDateTo(), newDateTo, id))
            throw new IllegalStateException("Room is already booked for the extra nights");
        booking.setDates(booking.getDateFrom(), newDateTo);
        bookingDao.update(booking);
    }

    // Walk-in at the front desk: the stay starts today and the guest is checked in
    // straight away ("Book & check in"). guestId 0 means a new guest, whose details
    // are checked and saved first. Everything is checked before anything is saved.
    // Returns the new booking id.
    public int walkInAndCheckIn(int guestId, String name, String nicPassport, String contact, String email,
                                int roomId, LocalDate dateTo) {
        LocalDate today = LocalDate.now();
        if (roomId <= 0)
            throw new IllegalArgumentException("Choose a room");
        if (dateTo == null)
            throw new IllegalArgumentException("Check-out date is required");
        if (!dateTo.isAfter(today))
            throw new IllegalArgumentException("Check-out must be after today");
        if (bookingDao.hasOverlap(roomId, today, dateTo, 0))
            throw new IllegalStateException("Room is already booked for these dates");

        if (guestId == 0) {
            checkNewGuest(name, nicPassport, contact, email);
            String cleanEmail = (email == null || email.isBlank()) ? null : email.trim();
            guestId = bookingDao.createGuest(name.trim(), nicPassport.trim(), contact.trim(), cleanEmail);
        }

        int id = createWalkIn(guestId, roomId, today, dateTo);
        checkIn(id);
        return id;
    }

    // Rooms free tonight for a walk-in, with their nightly rate
    public List<OptionItem> getAvailableRoomOptions(LocalDate from, LocalDate to) {
        return bookingDao.findAvailableRoomOptions(from, to);
    }

    // Dropdown choices for the booking form
    public List<OptionItem> getGuestOptions() {
        return bookingDao.findGuestOptions();
    }

    public List<OptionItem> getRoomOptions() {
        return bookingDao.findRoomOptions();
    }

    // ---------- status actions ----------
    // Booking itself checks that each status change is allowed,
    // so the service only loads, calls the method and saves.

    // CONFIRMED -> CHECKED_IN (also for the Check-in/out module)
    public void checkIn(int id) {
        Booking booking = getBooking(id);
        booking.checkIn();
        bookingDao.update(booking);
    }

    // CHECKED_IN -> CHECKED_OUT (also for the Check-in/out module)
    public void checkOut(int id) {
        Booking booking = getBooking(id);
        booking.checkOut();
        bookingDao.update(booking);
    }

    // CONFIRMED -> CANCELLED
    public void cancel(int id) {
        Booking booking = getBooking(id);
        booking.cancel();
        bookingDao.update(booking);
    }

    // Permanently removes a booking. Only canceled bookings can be deleted,
    // so the history of real stays is kept.
    public void deleteBooking(int id) {
        Booking booking = getBooking(id);
        if (booking.getStatus() != BookingStatus.CANCELLED)
            throw new IllegalStateException("Only a cancelled booking can be deleted");
        bookingDao.delete(id);
    }

    // Details for a new guest: name, NIC/passport and contact are required,
    // the NIC/passport must not belong to a saved guest, email is optional
    private void checkNewGuest(String name, String nicPassport, String contact, String email) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Guest full name is required");
        if (nicPassport == null || nicPassport.isBlank())
            throw new IllegalArgumentException("NIC / Passport no. is required");
        if (nicPassport.trim().length() > 20)
            throw new IllegalArgumentException("NIC / Passport no. is too long");
        if (contact == null || contact.isBlank())
            throw new IllegalArgumentException("Contact no. is required");
        if (email != null && !email.isBlank() && !email.contains("@"))
            throw new IllegalArgumentException("Email address is not valid");
        if (bookingDao.guestExistsByNic(nicPassport.trim()))
            throw new IllegalArgumentException("A guest with NIC / Passport " + nicPassport.trim()
                    + " is already saved. Pick them from the guest list.");
    }

    // Both dates must be given, and a booking cannot start in the past
    private void checkDates(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("Both dates are required");
        if (from.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Check-in date cannot be in the past");
    }
}
