package com.hotel.room;

/**
 * A double room for up to 2 guests.
 */
public class DoubleRoom extends Room {

    public DoubleRoom() {
        super();
    }

    public DoubleRoom(String roomId, String roomNumber, int floor, int capacity, double pricePerNight,
                      boolean hasAirConditioning, RoomStatus status, String description) {
        super(roomId, roomNumber, floor, capacity, pricePerNight, hasAirConditioning, status, description);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.DOUBLE;
    }

    @Override
    public int getMaxCapacity() {
        return 2;
    }
}
