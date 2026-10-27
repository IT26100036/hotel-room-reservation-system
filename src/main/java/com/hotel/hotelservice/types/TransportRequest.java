package com.hotel.hotelservice.types;

import com.hotel.hotelservice.ServiceRequest;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;
import com.hotel.hotelservice.enums.TripType;
import com.hotel.hotelservice.enums.VehicleType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public class TransportRequest extends ServiceRequest {

    public static final double CAR_RATE_PER_KM = 120.0;
    public static final double VAN_RATE_PER_KM = 180.0;

    private TripType tripType;
    private VehicleType vehicleType;
    private String location;

    private double distanceKm;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate travelDate;

    public TransportRequest() {
    }

    public TransportRequest(String requestId, String customerId, String notes, LocalDate requestDate,
                            RequestStatus status, TripType tripType, VehicleType vehicleType,
                            String location, double distanceKm, LocalDate travelDate) {
        super(requestId, customerId, notes, requestDate, status);
        setTripType(tripType);
        setVehicleType(vehicleType);
        setLocation(location);
        setDistanceKm(distanceKm);
        setTravelDate(travelDate);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.TRANSPORT;
    }

    /** Rate per km depends on the vehicle */
    public double getRatePerKm() {
        return vehicleType == VehicleType.VAN ? VAN_RATE_PER_KM : CAR_RATE_PER_KM;
    }

    @Override
    public double calculateCharge() {
        return distanceKm * getRatePerKm();
    }

    @Override
    public String getSummary() {
        return tripType + " by " + vehicleType + (tripType == TripType.DROP ? " to " : " from ")
                + location + " on " + travelDate;
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        if (tripType == null) {
            errors.add("Trip type is required");
        }
        if (vehicleType == null) {
            errors.add("Vehicle type is required");
        }
        if (location == null || location.trim().length() < 3 || location.trim().length() > 100) {
            errors.add("Location must be 3 to 100 characters");
        }
        if (distanceKm <= 0 || distanceKm > 500) {
            errors.add("Distance must be between 1 and 500 km");
        }
        if (travelDate == null) {
            errors.add("Travel date is required");
        } else if (travelDate.isBefore(LocalDate.now())) {
            errors.add("Travel date cannot be in the past");
        }
    }

    @Override
    public void copyDetailsFrom(ServiceRequest form) {
        TransportRequest t = (TransportRequest) form;
        setTripType(t.getTripType());
        setVehicleType(t.getVehicleType());
        setLocation(t.getLocation());
        setDistanceKm(t.getDistanceKm());
        setTravelDate(t.getTravelDate());
    }

    public TripType getTripType() {
        return tripType;
    }

    public void setTripType(TripType tripType) {
        this.tripType = tripType;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? null : location.trim();
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }
}
