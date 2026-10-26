package com.hotel.room;

/**
 * A suite for up to 4 guests. Adds a 10% service charge to the price.
 */
public class SuiteRoom extends Room {

    public static final double SERVICE_CHARGE_RATE = 0.10;

    public SuiteRoom() {
        super();
    }

    public SuiteRoom(String roomId, String roomNumber, int floor, int capacity, double pricePerNight,
                     boolean hasAirConditioning, RoomStatus status, String description) {
        super(roomId, roomNumber, floor, capacity, pricePerNight, hasAirConditioning, status, description);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SUITE;
    }

    @Override
    public int getMaxCapacity() {
        return 4;
    }

    /**
     * Suite price = pricePerNight x nights + 10% service charge.
     */
    @Override
    public double calculatePrice(int nights) {
        double base = super.calculatePrice(nights);
        return base + base * SERVICE_CHARGE_RATE;
    }
}
