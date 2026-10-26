package com.hotel.room;

/**
 * A single room for 1 guest.
 */
public class SingleRoom extends Room {

    public SingleRoom() {
        super();
    }

    public SingleRoom(String roomId, String roomNumber, int floor, int capacity, double pricePerNight,
                      boolean hasAirConditioning, RoomStatus status, String description) {
        super(roomId, roomNumber, floor, capacity, pricePerNight, hasAirConditioning, status, description);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SINGLE;
    }

    @Override
    public int getMaxCapacity() {
        return 1;
    }
}
