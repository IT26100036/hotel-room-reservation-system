package com.hotel.reservation.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Checks BookingDao against the test rows in MySQL (hotel_db).

@SpringBootTest
class BookingDaoTest {

    @Autowired
    private BookingDao bookingDao;

    // newest first, and each row is built as the right subclass
    @Test
    void findAllReturnsSampleBookings() {
        List<Booking> bookings = bookingDao.findAll();

        assertEquals(8, bookings.size());

        assertEquals(8, bookings.get(0).getId());
        assertInstanceOf(WalkInBooking.class, bookings.get(0));
        assertEquals(BookingStatus.CONFIRMED, bookings.get(0).getStatus());

        assertEquals(1, bookings.get(7).getId());
        assertInstanceOf(ReservationBooking.class, bookings.get(7));
    }

    @Test
    void findByIdReturnsReservationBookingWithAdvance() {
        Booking booking = bookingDao.findById(1);

        assertNotNull(booking);
        assertInstanceOf(ReservationBooking.class, booking);
        assertEquals(5000.0, ((ReservationBooking) booking).getAdvancePaid());
        assertEquals(3, booking.getNights());
        assertEquals(49000.0, booking.calculateAmountDue(54000));
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(bookingDao.findById(999));
    }

    // Walk-in: create -> read back -> check in and update -> delete
    @Test
    void createUpdateAndDeleteWalkInBooking() {
        Booking booking = new WalkInBooking(0, 3, 1,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), BookingStatus.CONFIRMED);

        int newId = bookingDao.create(booking);
        try {
            assertTrue(newId > 0);
            assertEquals(newId, booking.getId());

            Booking saved = bookingDao.findById(newId);
            assertInstanceOf(WalkInBooking.class, saved);
            assertEquals(1, saved.getRoomId());
            assertEquals(LocalDate.of(2026, 11, 3), saved.getDateTo());
            assertEquals(BookingStatus.CONFIRMED, saved.getStatus());

            saved.checkIn();
            assertTrue(bookingDao.update(saved));
            assertEquals(BookingStatus.CHECKED_IN, bookingDao.findById(newId).getStatus());
        } finally {
            assertTrue(bookingDao.delete(newId));
        }
        assertNull(bookingDao.findById(newId));
    }

    // Reservation booking keeps its link to reservation 1 and the Rs. 5,000 advance
    @Test
    void createReservationBookingKeepsReservationLink() {
        Booking booking = new ReservationBooking(0, 2, 2,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7), BookingStatus.CONFIRMED, 1, 5000);

        int newId = bookingDao.create(booking);
        try {
            Booking saved = bookingDao.findById(newId);
            assertInstanceOf(ReservationBooking.class, saved);
            assertEquals(1, ((ReservationBooking) saved).getReservationId());
            assertEquals(5000.0, ((ReservationBooking) saved).getAdvancePaid());
        } finally {
            bookingDao.delete(newId);
        }
    }

    // Updating or deleting an id that does not exist returns false
    @Test
    void updateAndDeleteReturnFalseWhenMissing() {
        Booking ghost = new WalkInBooking(999, 1, 1,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), BookingStatus.CONFIRMED);
        assertFalse(bookingDao.update(ghost));
        assertFalse(bookingDao.delete(999));
    }
}
