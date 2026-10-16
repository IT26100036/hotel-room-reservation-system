package com.hotel.reservation.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hotel.reservation.common.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingDao bookingDao;

    // Used only to remove guests created by the walk-in tests
    @Autowired
    private DBConnection dbConnection;

    private static final int ROOM = 2;
    private static final int GUEST = 3;

    private LocalDate daysFromToday(int days) {
        return LocalDate.now().plusDays(days);
    }

    @Test
    void getBookingReturnsSampleBooking() {
        Booking booking = bookingService.getBooking(1);
        assertInstanceOf(ReservationBooking.class, booking);
    }

    @Test
    void getBookingThrowsWhenMissing() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> bookingService.getBooking(999));
        assertEquals("Booking not found: 999", e.getMessage());
    }

    @Test
    void getBookingsByStatusReturnsOnlyThatStatus() {
        for (Booking b : bookingService.getBookingsByStatus(BookingStatus.CHECKED_IN)) {
            assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        }
    }

    @Test
    void createWalkInRejectsPastDate() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWalkIn(GUEST, ROOM, daysFromToday(-1), daysFromToday(1)));
        assertEquals("Check-in date cannot be in the past", e.getMessage());
    }

    @Test
    void createWalkInRejectsMissingDate() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWalkIn(GUEST, ROOM, null, daysFromToday(2)));
    }

    @Test
    void createWalkInRejectsCheckOutBeforeCheckIn() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWalkIn(GUEST, ROOM, daysFromToday(32), daysFromToday(30)));
    }

    @Test
    void createWalkInSavesAndBlocksSameRoom() {
        int firstId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(30), daysFromToday(33));
        int backToBackId = 0;
        try {
            Booking saved = bookingService.getBooking(firstId);
            assertInstanceOf(WalkInBooking.class, saved);
            assertEquals(BookingStatus.CONFIRMED, saved.getStatus());
            assertEquals(3, saved.getNights());

            Exception e = assertThrows(IllegalStateException.class,
                    () -> bookingService.createWalkIn(GUEST, ROOM, daysFromToday(31), daysFromToday(32)));
            assertEquals("Room is already booked for these dates", e.getMessage());

            backToBackId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(33), daysFromToday(35));
            assertTrue(backToBackId > 0);
        } finally {
            bookingDao.delete(firstId);
            if (backToBackId > 0)
                bookingDao.delete(backToBackId);
        }
    }

    @Test
    void updateBookingChangesRoomAndDates() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(40), daysFromToday(42));
        try {
            bookingService.updateBooking(id, 1, daysFromToday(41), daysFromToday(44));

            Booking updated = bookingService.getBooking(id);
            assertEquals(1, updated.getRoomId());
            assertEquals(daysFromToday(41), updated.getDateFrom());
            assertEquals(daysFromToday(44), updated.getDateTo());
        } finally {
            bookingDao.delete(id);
        }
    }

    @Test
    void updateBookingAllowsKeepingOwnDates() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(50), daysFromToday(52));
        try {
            assertDoesNotThrow(() -> bookingService.updateBooking(id, ROOM, daysFromToday(50), daysFromToday(52)));
        } finally {
            bookingDao.delete(id);
        }
    }

    @Test
    void updateBookingRejectsClashAndKeepsOldData() {
        int firstId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(60), daysFromToday(62));
        int secondId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(65), daysFromToday(67));
        try {
            assertThrows(IllegalStateException.class,
                    () -> bookingService.updateBooking(secondId, ROOM, daysFromToday(61), daysFromToday(66)));

            Booking unchanged = bookingService.getBooking(secondId);
            assertEquals(daysFromToday(65), unchanged.getDateFrom());
            assertEquals(daysFromToday(67), unchanged.getDateTo());
        } finally {
            bookingDao.delete(firstId);
            bookingDao.delete(secondId);
        }
    }

    @Test
    void updateBookingRejectsPastDate() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(70), daysFromToday(72));
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> bookingService.updateBooking(id, ROOM, daysFromToday(-2), daysFromToday(1)));
        } finally {
            bookingDao.delete(id);
        }
    }

    @Test
    void updateBookingRejectsBookingThatIsNotConfirmed() {
        Exception e = assertThrows(IllegalStateException.class,
                () -> bookingService.updateBooking(2, ROOM, daysFromToday(80), daysFromToday(82)));
        assertEquals("Only a confirmed booking can be changed", e.getMessage());
    }

    // ---------- status actions ----------

    // CONFIRMED -> CHECKED_IN -> CHECKED_OUT, saved in the database each time
    @Test
    void checkInThenCheckOutMovesStatusForward() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(90), daysFromToday(92));
        try {
            bookingService.checkIn(id);
            assertEquals(BookingStatus.CHECKED_IN, bookingService.getBooking(id).getStatus());

            bookingService.checkOut(id);
            assertEquals(BookingStatus.CHECKED_OUT, bookingService.getBooking(id).getStatus());
        } finally {
            bookingDao.delete(id);
        }
    }

    // A cancelled booking can be deleted, and is gone afterwards
    @Test
    void cancelThenDeleteRemovesBooking() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(95), daysFromToday(97));
        try {
            bookingService.cancel(id);
            assertEquals(BookingStatus.CANCELLED, bookingService.getBooking(id).getStatus());

            bookingService.deleteBooking(id);
            assertThrows(IllegalArgumentException.class, () -> bookingService.getBooking(id));
        } finally {
            bookingDao.delete(id);   // does nothing if the test already deleted it
        }
    }

    // Sample booking 4 is CHECKED_OUT, so it cannot be checked in again
    @Test
    void checkInRejectsCheckedOutBooking() {
        assertThrows(IllegalStateException.class, () -> bookingService.checkIn(4));
        assertEquals(BookingStatus.CHECKED_OUT, bookingService.getBooking(4).getStatus());
    }

    // Sample booking 2 is CHECKED_IN, so it cannot be cancelled
    @Test
    void cancelRejectsCheckedInBooking() {
        assertThrows(IllegalStateException.class, () -> bookingService.cancel(2));
        assertEquals(BookingStatus.CHECKED_IN, bookingService.getBooking(2).getStatus());
    }

    // Sample booking 8 is CONFIRMED, so it cannot be deleted
    @Test
    void deleteRejectsBookingThatIsNotCancelled() {
        Exception e = assertThrows(IllegalStateException.class, () -> bookingService.deleteBooking(8));
        assertEquals("Only a cancelled booking can be deleted", e.getMessage());
        assertNotNull(bookingService.getBooking(8));
    }

    // ---------- extend stay ----------

    // A checked-in guest stays two more nights
    @Test
    void extendStayMovesCheckOutLater() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(100), daysFromToday(102));
        try {
            bookingService.checkIn(id);
            bookingService.extendStay(id, daysFromToday(104));

            Booking extended = bookingService.getBooking(id);
            assertEquals(daysFromToday(100), extended.getDateFrom());
            assertEquals(daysFromToday(104), extended.getDateTo());
        } finally {
            bookingDao.delete(id);
        }
    }

    // The new check-out must be later than the current one
    @Test
    void extendStayRejectsSameOrEarlierDate() {
        int id = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(105), daysFromToday(107));
        try {
            bookingService.checkIn(id);
            assertThrows(IllegalArgumentException.class, () -> bookingService.extendStay(id, daysFromToday(107)));
            assertThrows(IllegalArgumentException.class, () -> bookingService.extendStay(id, daysFromToday(106)));
        } finally {
            bookingDao.delete(id);
        }
    }

    // The extra nights clash with the next guest's booking
    @Test
    void extendStayRejectsClashWithNextBooking() {
        int firstId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(110), daysFromToday(112));
        int nextId = bookingService.createWalkIn(GUEST, ROOM, daysFromToday(113), daysFromToday(115));
        try {
            bookingService.checkIn(firstId);
            assertThrows(IllegalStateException.class, () -> bookingService.extendStay(firstId, daysFromToday(114)));
            assertEquals(daysFromToday(112), bookingService.getBooking(firstId).getDateTo());
        } finally {
            bookingDao.delete(firstId);
            bookingDao.delete(nextId);
        }
    }

    // Sample booking 8 is CONFIRMED, not checked in, so it uses Edit instead
    @Test
    void extendStayRejectsBookingThatIsNotCheckedIn() {
        assertThrows(IllegalStateException.class, () -> bookingService.extendStay(8, daysFromToday(200)));
    }

    // ---------- dropdowns ----------

    @Test
    void guestOptionsIncludeSampleGuest() {
        boolean found = false;
        for (OptionItem o : bookingService.getGuestOptions()) {
            if (o.getId() == 2 && o.getLabel().equals("Anjali Fernando (199545612345)"))
                found = true;
        }
        assertTrue(found);
    }

    // Room 5 (108) is under maintenance, so it is not offered
    @Test
    void roomOptionsSkipMaintenanceRooms() {
        boolean has105 = false;
        for (OptionItem o : bookingService.getRoomOptions()) {
            assertNotEquals(5, o.getId());
            if (o.getId() == 3 && o.getLabel().equals("105 - Double"))
                has105 = true;
        }
        assertTrue(has105);
    }

    // ---------- walk-in: book and check in ----------

    private void deleteGuest(int guestId) throws Exception {
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM guest WHERE guest_id = ?")) {
            ps.setInt(1, guestId);
            ps.executeUpdate();
        }
    }

    // Existing guest: the stay starts today and the guest is checked in at once
    @Test
    void walkInExistingGuestIsCheckedInFromToday() {
        int id = bookingService.walkInAndCheckIn(GUEST, null, null, null, null, ROOM, daysFromToday(2));
        try {
            Booking b = bookingService.getBooking(id);
            assertInstanceOf(WalkInBooking.class, b);
            assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
            assertEquals(LocalDate.now(), b.getDateFrom());
            assertEquals(daysFromToday(2), b.getDateTo());
        } finally {
            bookingDao.delete(id);
        }
    }

    // New guest: the guest is saved first, then booked and checked in
    @Test
    void walkInNewGuestSavesGuestFirst() throws Exception {
        String nic = "T" + System.currentTimeMillis();   // unique each run
        int id = bookingService.walkInAndCheckIn(0, "Test Walkin", nic, "0771234567", "", ROOM, daysFromToday(1));
        Booking b = bookingService.getBooking(id);
        try {
            assertEquals("Test Walkin", b.getGuestName());
            assertEquals(nic, b.getGuestNicPassport());
            assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        } finally {
            bookingDao.delete(id);
            deleteGuest(b.getGuestId());
        }
    }

    // NIC already saved (Anjali): nothing is saved, the user is told to pick the guest
    @Test
    void walkInRejectsNicThatIsAlreadySaved() {
        int before = bookingService.getAllBookings().size();
        assertThrows(IllegalArgumentException.class, () -> bookingService.walkInAndCheckIn(
                0, "Someone Else", "199545612345", "0770000000", null, ROOM, daysFromToday(1)));
        assertEquals(before, bookingService.getAllBookings().size());
    }

    @Test
    void walkInRejectsNewGuestWithoutName() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> bookingService.walkInAndCheckIn(
                0, " ", "T123", "0770000000", null, ROOM, daysFromToday(1)));
        assertEquals("Guest full name is required", e.getMessage());
    }

    // The room already has a booking tonight
    @Test
    void walkInRejectsRoomBookedTonight() {
        int existing = bookingService.createWalkIn(GUEST, ROOM, LocalDate.now(), daysFromToday(3));
        try {
            assertThrows(IllegalStateException.class,
                    () -> bookingService.walkInAndCheckIn(GUEST, null, null, null, null, ROOM, daysFromToday(1)));
        } finally {
            bookingDao.delete(existing);
        }
    }

    // A room booked tonight and a room under maintenance are not offered
    @Test
    void availableRoomsSkipBookedAndMaintenanceRooms() {
        int existing = bookingService.createWalkIn(GUEST, ROOM, LocalDate.now(), daysFromToday(2));
        try {
            for (OptionItem o : bookingService.getAvailableRoomOptions(LocalDate.now(), daysFromToday(1))) {
                assertNotEquals(ROOM, o.getId());
                assertNotEquals(5, o.getId());
                assertTrue(o.getRate() > 0);
            }
        } finally {
            bookingDao.delete(existing);
        }
    }
}
