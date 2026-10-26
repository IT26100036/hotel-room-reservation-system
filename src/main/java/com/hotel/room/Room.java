package com.hotel.room;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every hotel room.
 * SingleRoom, DoubleRoom and SuiteRoom extend this class.
 * It is abstract, so a plain Room can't be created.
 */
public abstract class Room {

    protected String roomId;
    protected String roomNumber;
    protected int floor;
    protected int capacity;
    protected double pricePerNight;
    protected boolean hasAirConditioning;
    protected RoomStatus status;
    protected String description;

    public Room() {
    }

    public Room(String roomId, String roomNumber, int floor, int capacity, double pricePerNight,
                boolean hasAirConditioning, RoomStatus status, String description) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.hasAirConditioning = hasAirConditioning;
        this.status = status;
        this.description = description;
    }

    /** Returns SINGLE, DOUBLE or SUITE. */
    public abstract RoomType getRoomType();

    /** The most guests this type of room can hold. */
    public abstract int getMaxCapacity();

    /**
     * Price for a stay. SuiteRoom overrides this to add its service charge.
     */
    public double calculatePrice(int nights) {
        return pricePerNight * nights;
    }

    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    /**
     * Validates room attributes according to specification rules.
     * Returns a list of error messages; an empty list means the room is valid.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // RoomNumber: Required, 3 to 4 digits
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            errors.add("Room number is required");
        } else if (!roomNumber.trim().matches("^[0-9]{3,4}$")) {
            errors.add("Room number must be 3 to 4 digits (for example 101)");
        }

        // Floor: Required, 0 to 50
        if (floor < 0 || floor > 50) {
            errors.add("Floor must be between 0 and 50");
        }

        // Capacity: Required, 1 up to the room type's max
        if (capacity < 1 || capacity > getMaxCapacity()) {
            errors.add("Capacity for a " + getRoomType() + " room must be between 1 and " + getMaxCapacity());
        }

        // PricePerNight: Required, greater than 0
        if (pricePerNight <= 0) {
            errors.add("Price per night must be greater than 0");
        }

        // Status: Required
        if (status == null) {
            errors.add("Status is required");
        }

        // Description: Optional, max 255 characters
        if (description != null && description.length() > 255) {
            errors.add("Description cannot exceed 255 characters");
        }

        return errors;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public boolean isHasAirConditioning() {
        return hasAirConditioning;
    }

    public void setHasAirConditioning(boolean hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
