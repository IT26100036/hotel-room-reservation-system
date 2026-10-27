package com.hotel.finance.methods;

import com.hotel.finance.Payment;
import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;

import java.time.LocalDate;
import java.util.List;

public class Online extends Payment {
    private String referenceNo;

    public Online() {
    }

    public Online(String paymentId, String invoiceId, double amount,
                  PaymentType paymentType, LocalDate paymentDate, String referenceNo) {
        super(paymentId, invoiceId, amount, paymentType, paymentDate);
        setReferenceNo(referenceNo);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.ONLINE;
    }

    @Override
    protected void validateMethodRules(List<String> errors) {
        if (referenceNo == null || !referenceNo.trim().matches("^[A-Za-z0-9]{6,20}$")) {
            errors.add("Reference number must be 6 to 20 letters or digits");
        }
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}
