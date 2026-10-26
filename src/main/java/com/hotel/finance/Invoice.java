package com.hotel.finance;

import com.hotel.finance.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    /** Attributes */
    private String invoiceId;        // INV001, INV002...
    private String bookingId;        // B001...
    private String customerId;       // C001...
    private double roomCharge;       // nights x room price
    private double extraCharges;     // anything added later (food, damages...)
    private double discountPercent;  // copied from the customer, 0 to 100
    private double amountPaid;       // total of all payments so far
    private InvoiceStatus status;
    private LocalDate issuedDate;

    /** Constructors */
    public Invoice() {
    }

    public Invoice(String invoiceId, String bookingId, String customerId, double roomCharge,
                   double extraCharges, double discountPercent, double amountPaid,
                   InvoiceStatus status, LocalDate issuedDate) {
        setInvoiceId(invoiceId);
        setBookingId(bookingId);
        setCustomerId(customerId);
        setRoomCharge(roomCharge);
        setExtraCharges(extraCharges);
        setDiscountPercent(discountPercent);
        setAmountPaid(amountPaid);
        setStatus(status);
        setIssuedDate(issuedDate);
    }

    /** Calculations */
    public double getSubtotal() {
        return roomCharge + extraCharges;
    }

    public double getDiscountAmount() {
        return getSubtotal() * discountPercent / 100;
    }

    public double getTotal() {
        return getSubtotal() - getDiscountAmount();
    }

    public double getBalance() {
        return getTotal() - amountPaid;
    }

    /** Adds a payment and updates the status */
    public void addPayment(double amount) {
        amountPaid += amount;
        updateStatus();
    }

    /** UNPAID, PARTIALLY_PAID or PAID, worked out from the balance. A VOID invoice stays VOID. */
    public void updateStatus() {
        if (status == InvoiceStatus.VOID) {
            return;
        }
        if (amountPaid <= 0) {
            status = InvoiceStatus.UNPAID;
        } else if (getBalance() > 0) {
            status = InvoiceStatus.PARTIALLY_PAID;
        } else {
            status = InvoiceStatus.PAID;
        }
    }

    /** Checks all rules and returns every error found */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (bookingId == null || bookingId.trim().isEmpty()) {
            errors.add("Booking ID is required");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer ID is required");
        }
        if (roomCharge <= 0) {
            errors.add("Room charge must be greater than 0");
        }
        if (extraCharges < 0) {
            errors.add("Extra charges cannot be negative");
        }
        if (discountPercent < 0 || discountPercent > 100) {
            errors.add("Discount must be between 0 and 100");
        }
        if (amountPaid < 0) {
            errors.add("Amount paid cannot be negative");
        }
        if (amountPaid > getTotal()) {
            errors.add("Amount paid cannot be more than the total");
        }
        return errors;
    }

    /** Getters and Setters */
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public void setRoomCharge(double roomCharge) {
        this.roomCharge = roomCharge;
    }

    public double getExtraCharges() {
        return extraCharges;
    }

    public void setExtraCharges(double extraCharges) {
        this.extraCharges = extraCharges;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }
}
