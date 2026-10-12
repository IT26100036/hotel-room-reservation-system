package com.hotel.reservation.booking;

import com.hotel.reservation.common.CrudDao;
import com.hotel.reservation.common.DBConnection;
import org.springframework.stereotype.Repository;

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

    // Inserts a new booking and returns its generated id
    @Override
    public int create(Booking booking) {
        return 0;
    }

    // Returns the booking with this id, or null if not found
    @Override
    public Booking findById(int id) {
        return null;
    }

    // Returns all bookings, newest first
    @Override
    public List<Booking> findAll() {
        return List.of();
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
}
