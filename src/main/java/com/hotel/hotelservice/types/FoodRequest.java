package com.hotel.hotelservice.types;

import com.hotel.hotelservice.ServiceRequest;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;

import java.time.LocalDate;
import java.util.List;

public class FoodRequest extends ServiceRequest {

    private String itemName;
    private int quantity;
    private double unitPrice;

    public FoodRequest() {
    }

    public FoodRequest(String requestId, String customerId, String notes, LocalDate requestDate,
                       RequestStatus status, String itemName, int quantity, double unitPrice) {
        super(requestId, customerId, notes, requestDate, status);
        setItemName(itemName);
        setQuantity(quantity);
        setUnitPrice(unitPrice);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.FOOD;
    }

    @Override
    public double calculateCharge() {
        return quantity * unitPrice;
    }

    @Override
    public String getSummary() {
        return quantity + " x " + itemName;
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        if (itemName == null || !itemName.trim().matches("^[a-zA-Z ]{2,60}$")) {
            errors.add("Item name must be 2 to 60 letters");
        }
        if (quantity < 1 || quantity > 50) {
            errors.add("Quantity must be between 1 and 50");
        }
        if (unitPrice <= 0) {
            errors.add("Unit price must be greater than 0");
        }
    }

    @Override
    public void copyDetailsFrom(ServiceRequest form) {
        FoodRequest f = (FoodRequest) form;
        setItemName(f.getItemName());
        setQuantity(f.getQuantity());
        setUnitPrice(f.getUnitPrice());
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName == null ? null : itemName.trim();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
}
