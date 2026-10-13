package com.hotel.reservation.booking;

import com.hotel.reservation.common.CrudDao;
import com.hotel.reservation.common.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Reads and writes the booking table in MySQL
// The only class in the booking module that runs SQL

@Repository
public class BookingDao implements CrudDao<Booking> {
    // Shared MySQL connection helper, given by Spring (constructor injection)
    private final DBConnection dbConnection;

    public BookingDao(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // Booking columns plus the advance from the linked reservation.
    // LEFT JOIN keeps walk-in bookings, which have no reservation.
    private static final String SELECT_BOOKINGS = "SELECT b.booking_id, b.reservation_id, b.guest_id, b.room_id, b.date_from, b.date_to, "
            + "b.booking_type, b.status, r.advance_amount "
            + "FROM booking b LEFT JOIN reservation r ON b.reservation_id = r.reservation_id";

    // Inserts a new booking and returns its generated id
    @Override
    public int create(Booking booking) {
        return 0;
    }

    // Returns the booking with this id, or null if not found
    @Override
    public Booking findById(int id) {
        String sql = SELECT_BOOKINGS + " WHERE b.booking_id = ?";
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load booking " + id, e);
        }
    }

    // Returns all bookings, newest first
    @Override
    public List<Booking> findAll() {
        String sql = SELECT_BOOKINGS + " ORDER BY b.booking_id DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                bookings.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not load bookings", e);
        }
        return bookings;
    }

    // Saves changes to room, dates and status
    @Override
    public boolean update(Booking booking) {
        return false;
    }

    // Permanently removes a booking (only used for canceled bookings)
    @Override
    public boolean delete(int id) {
        return false;
    }

    // Turns one database row into the right kind of Booking object
    // booking_type decides which subclass to create
    private Booking mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("booking_id");
        int guestId = rs.getInt("guest_id");
        int roomId = rs.getInt("room_id");
        LocalDate dateFrom = rs.getObject("date_from", LocalDate.class);
        LocalDate dateTo = rs.getObject("date_to", LocalDate.class);
        BookingStatus status = BookingStatus.valueOf(rs.getString("status"));

        if ("RESERVATION".equals(rs.getString("booking_type"))) {
            return new ReservationBooking(id, guestId, roomId, dateFrom, dateTo, status,
                    rs.getInt("reservation_id"), rs.getDouble("advance_amount"));
        }
        return new WalkInBooking(id, guestId, roomId, dateFrom, dateTo, status);
    }
}
