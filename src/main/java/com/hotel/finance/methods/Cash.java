package com.hotel.finance.methods;

import com.hotel.finance.Payment;
import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;

import java.time.LocalDate;
import java.util.List;

public class Cash extends Payment {
    private double amountReceived;

    public Cash() {
    }

    public Cash(String paymentId, String invoiceId, double amount,
                PaymentType paymentType, LocalDate paymentDate, double amountReceived) {
        super(paymentId, invoiceId, amount, paymentType, paymentDate);
        setAmountReceived(amountReceived);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CASH;
    }

    @Override
    protected void validateMethodRules(List<String> errors) {
        if (amountReceived < getAmount()) {
            errors.add("Cash received cannot be less than the amount");
        }
    }

    public double getChange() {
        return amountReceived - getAmount();
    }

    public double getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(double amountReceived) {
        this.amountReceived = amountReceived;
    }
}
