package com.hotel.finance;

import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Payment {
    /** Attributes */
    private String paymentId;      // PAY001, PAY002...
    private String invoiceId;      // INV001, INV002...
    private double amount;
    private PaymentType paymentType;
    private LocalDate paymentDate;

    /** Constructors */
    public Payment() {
    }

    public Payment(String paymentId, String invoiceId, double amount, PaymentType paymentType, LocalDate paymentDate) {
        setPaymentId(paymentId);
        setInvoiceId(invoiceId);
        setAmount(amount);
        setPaymentType(paymentType);
        setPaymentDate(paymentDate);
    }

    /** Abstract Methods */
    // CASH, CARD, ONLINE
    public abstract PaymentMethod getPaymentMethod();

    // Extra rules that only one payment method has
    protected abstract void validateMethodRules(List<String> errors);

    /** Checks all rules and returns every error found */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            errors.add("Invoice ID is required");
        }
        if (amount <= 0) {
            errors.add("Amount must be greater than 0");
        }
        if (paymentType == null) {
            errors.add("Payment type is required");
        }
        if (paymentDate != null && paymentDate.isAfter(LocalDate.now())) {
            errors.add("Payment date cannot be in the future");
        }

        validateMethodRules(errors);
        return errors;
    }

    /** Getters and Setters */
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}
