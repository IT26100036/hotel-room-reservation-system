package com.hotel.reservation.booking;

import com.hotel.reservation.common.CrudDao;
import com.hotel.reservation.common.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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

    // LEFT JOIN keeps walk-in bookings, which have no reservation.
    private static final String SELECT_BOOKINGS = "SELECT b.booking_id, b.reservation_id, b.guest_id, b.room_id, b.date_from, b.date_to, "
            + "b.booking_type, b.status, r.advance_amount, g.name AS guest_name, g.nic_passport, rm.room_no "
            + "FROM booking b "
            + "LEFT JOIN reservation r ON b.reservation_id = r.reservation_id "
            + "LEFT JOIN guest g ON b.guest_id = g.guest_id "
            + "LEFT JOIN room rm ON b.room_id = rm.room_id";

    // Inserts a new booking and returns its generated id
    @Override
    public int create(Booking booking) {
        String sql = "INSERT INTO booking "
                + "(reservation_id, guest_id, room_id, date_from, date_to, booking_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {

            // Only a ReservationBooking is linked to a reservation; walk-ins save NULL
            if (booking instanceof ReservationBooking reservationBooking) {
                ps.setInt(1, reservationBooking.getReservationId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, booking.getGuestId());
            ps.setInt(3, booking.getRoomId());
            ps.setObject(4, booking.getDateFrom());
            ps.setObject(5, booking.getDateTo());
            ps.setString(6, booking.getBookingType());
            ps.setString(7, booking.getStatus().name());
            ps.executeUpdate();

            // Read the id MySQL generated and store it in the object
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    booking.setId(newId);
                    return newId;
                }
            }
            throw new RuntimeException("Booking was not saved");
        } catch (SQLException e) {
            throw new RuntimeException("Could not save booking", e);
        }
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

    // Returns only the bookings with this status, newest first
    public List<Booking> findByStatus(BookingStatus status) {
        String sql = SELECT_BOOKINGS + " WHERE b.status = ? ORDER BY b.booking_id DESC";
        List<Booking> bookings = new ArrayList<>();

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    bookings.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load " + status + " bookings", e);
        }
        return bookings;
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
        String sql = "UPDATE booking SET room_id = ?, date_from = ?, date_to = ?, status = ? "
                + "WHERE booking_id = ?";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, booking.getRoomId());
            ps.setObject(2, booking.getDateFrom());
            ps.setObject(3, booking.getDateTo());
            ps.setString(4, booking.getStatus().name());
            ps.setInt(5, booking.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update booking " + booking.getId(), e);
        }
    }

    // Permanently removes a booking (only used for canceled bookings)
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM booking WHERE booking_id = ?";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete booking " + id, e);
        }
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
        int reservationId = rs.getInt("reservation_id");
        double advanceAmount = rs.getDouble("advance_amount");

        Booking booking;

        if ("RESERVATION".equals(rs.getString("booking_type")))
            booking = new ReservationBooking(id, guestId, roomId, dateFrom, dateTo, status, reservationId,
                    advanceAmount);
        else
            booking = new WalkInBooking(id, guestId, roomId, dateFrom, dateTo, status);
        booking.setGuestName(rs.getString("guest_name"));
        booking.setRoomNo(rs.getString("room_no"));
        booking.setGuestNicPassport(rs.getString("nic_passport"));
        return booking;
    }

    // True if another active booking holds this room on any of these nights
    // Canceled and checked-out bookings do not block the room
    // A stay ending on the day another starts is not a clash (strict < and >)
    public boolean hasOverlap(int roomId, LocalDate from, LocalDate to, int excludeBookingId) {
        String sql = "SELECT COUNT(*) FROM booking "
                + "WHERE room_id = ? "
                + "AND status NOT IN ('CANCELLED', 'CHECKED_OUT') "
                + "AND date_from < ? AND date_to > ? "
                + "AND booking_id <> ?";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setObject(2, to);
            ps.setObject(3, from);
            ps.setInt(4, excludeBookingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check room " + roomId + " for overlaps", e);
        }
    }

    // ---------- dropdown data for the booking form ----------
    // Read-only lookups in the guest and room tables.
    // Can be swapped for the Guest and Room modules' DAOs when they are ready.

    // All guests, A to Z: "Anjali Fernando (199545612345)"
    public List<OptionItem> findGuestOptions() {
        String sql = "SELECT guest_id, name, nic_passport FROM guest ORDER BY name";

        List<OptionItem> options = new ArrayList<>();
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                options.add(new OptionItem(rs.getInt("guest_id"),
                        rs.getString("name") + " (" + rs.getString("nic_passport") + ")"));
        } catch (SQLException e) {
            throw new RuntimeException("Could not load guests", e);
        }
        return options;
    }

    // Rooms that can be booked (not under maintenance), by room number: "105 - Double"
    // Free dates are still checked by hasOverlap when saving
    public List<OptionItem> findRoomOptions() {
        String sql = "SELECT room_id, room_no, type FROM room WHERE status <> 'MAINTENANCE' ORDER BY room_no";

        List<OptionItem> options = new ArrayList<>();
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("type");
                // SUITE -> Suite
                String niceType = type.charAt(0) + type.substring(1).toLowerCase();
                options.add(new OptionItem(rs.getInt("room_id"), rs.getString("room_no") + " - " + niceType));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load rooms", e);
        }
        return options;
    }

    // Rooms free right now for a walk-in: not under maintenance and no active booking
    // on the given nights. Includes the nightly rate for the "Total amount due" box.
    public List<OptionItem> findAvailableRoomOptions(LocalDate from, LocalDate to) {
        String sql = "SELECT rm.room_id, rm.room_no, rm.type, rm.rate FROM room rm "
                + "WHERE rm.status <> 'MAINTENANCE' "
                + "AND NOT EXISTS (SELECT 1 FROM booking b WHERE b.room_id = rm.room_id "
                + "AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT') "
                + "AND b.date_from < ? AND b.date_to > ?) "
                + "ORDER BY rm.room_no";

        List<OptionItem> options = new ArrayList<>();
        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, to);
            ps.setObject(2, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    String niceType = type.charAt(0) + type.substring(1).toLowerCase();
                    options.add(new OptionItem(rs.getInt("room_id"),
                            rs.getString("room_no") + " - " + niceType, rs.getDouble("rate")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load available rooms", e);
        }
        return options;
    }

    // ---------- new guest at the desk ----------
    // Small write into the guest table so a walk-in can be booked in one step.
    // Can be swapped for the Guest module's DAO when it is ready.

    // True if a guest with this NIC or passport number is already saved
    public boolean guestExistsByNic(String nicPassport) {
        String sql = "SELECT COUNT(*) FROM guest WHERE nic_passport = ?";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nicPassport);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check guest " + nicPassport, e);
        }
    }

    // Saves a new guest and returns the generated guest id
    public int createGuest(String name, String nicPassport, String contact, String email) {
        String sql = "INSERT INTO guest (name, nic_passport, contact, email) VALUES (?, ?, ?, ?)";

        try (Connection con = dbConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, nicPassport);
            ps.setString(3, contact);
            ps.setString(4, email);   // null when not given
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Guest was not saved");
        } catch (SQLException e) {
            throw new RuntimeException("Could not save guest", e);
        }
    }
}
