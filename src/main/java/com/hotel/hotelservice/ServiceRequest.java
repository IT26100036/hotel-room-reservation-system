package com.hotel.hotelservice;

import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class ServiceRequest {
    private String requestId;
    private String customerId;
    private String notes;
    private LocalDate requestDate;
    private RequestStatus status;

    public ServiceRequest() {
    }

    public ServiceRequest(String requestId, String customerId, String notes,
                          LocalDate requestDate, RequestStatus status) {
        setRequestId(requestId);
        setCustomerId(customerId);
        setNotes(notes);
        setRequestDate(requestDate);
        setStatus(status);
    }

    /** Abstract methods */
    public abstract ServiceType getServiceType();
    public abstract double calculateCharge();
    public abstract String getSummary();
    protected abstract void validateTypeRules(List<String> errors);
    public abstract void copyDetailsFrom(ServiceRequest form);

    /** Allowed status changes: REQUESTED -> IN_PROGRESS -> COMPLETED, and cancel before completion */
    public boolean canMoveTo(RequestStatus newStatus) {
        if (status == RequestStatus.REQUESTED) {
            return newStatus == RequestStatus.IN_PROGRESS || newStatus == RequestStatus.CANCELLED;
        }
        if (status == RequestStatus.IN_PROGRESS) {
            return newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.CANCELLED;
        }
        return false;
    }

    /** Only a request that has not started can be edited */
    public boolean isEditable() {
        return status == RequestStatus.REQUESTED;
    }

    /** Checks all rules and returns every error found */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer is required");
        }
        if (notes != null && notes.length() > 255) {
            errors.add("Notes cannot be longer than 255 characters");
        }

        validateTypeRules(errors);
        return errors;
    }

    /** Getters and Setters */
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes == null || notes.trim().isEmpty()) ? null : notes.trim();
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
