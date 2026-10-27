package com.hotel.hotelservice.types;

import com.hotel.hotelservice.ServiceRequest;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;

import java.time.LocalDate;
import java.util.List;

public class LaundryRequest extends ServiceRequest {

    public static final double PRICE_PER_ITEM = 250.0;
    public static final double EXPRESS_EXTRA_RATE = 0.50;

    private int itemCount;
    private boolean express;

    public LaundryRequest() {
    }

    public LaundryRequest(String requestId, String customerId, String notes, LocalDate requestDate,
                          RequestStatus status, int itemCount, boolean express) {
        super(requestId, customerId, notes, requestDate, status);
        setItemCount(itemCount);
        setExpress(express);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.LAUNDRY;
    }

    @Override
    public double calculateCharge() {
        double charge = itemCount * PRICE_PER_ITEM;
        if (express) {
            charge += charge * EXPRESS_EXTRA_RATE;
        }
        return charge;
    }

    @Override
    public String getSummary() {
        return itemCount + " item(s)" + (express ? " - express" : "");
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        if (itemCount < 1 || itemCount > 100) {
            errors.add("Number of items must be between 1 and 100");
        }
    }

    @Override
    public void copyDetailsFrom(ServiceRequest form) {
        LaundryRequest l = (LaundryRequest) form;
        setItemCount(l.getItemCount());
        setExpress(l.isExpress());
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public boolean isExpress() {
        return express;
    }

    public void setExpress(boolean express) {
        this.express = express;
    }
}
