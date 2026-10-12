package com.hotel.reservation.booking;

// CONFIRMED -> CHECKED_IN -> CHECKED_OUT
// CONFIRMED -> CANCELLED

public enum BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
