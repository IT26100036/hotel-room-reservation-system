package com.hotel.booking;

public enum BookingStatus {
    PENDING,       // reservation waiting for the advance payment (24 hours)
    CONFIRMED,     // ready for check-in
    CHECKED_IN,    // guest is in the room
    CHECKED_OUT,   // stay finished
    CANCELLED,     // cancelled by staff, or a PENDING booking that expired
    NO_SHOW        // CONFIRMED but the guest did not arrive on the check-in date
}
