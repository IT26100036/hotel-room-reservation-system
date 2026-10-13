package com.hotel.reservation.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Checks BookingDao against the test rows in MySQL (hotel_db).

@SpringBootTest
class BookingDaoTest {

    @Autowired
    private BookingDao bookingDao;

    @Test
    void findAllReturnsBothBookings() {
        List<Booking> bookings = bookingDao.findAll();

        assertEquals(2, bookings.size());
        assertInstanceOf(WalkInBooking.class, bookings.get(0));
        assertEquals(BookingStatus.CHECKED_IN, bookings.get(0).getStatus());
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
}