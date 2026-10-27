package com.hotel.finance.methods;

import com.hotel.finance.Payment;
import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;

import java.time.LocalDate;
import java.util.List;

public class Card extends Payment {
    private String cardHolderName;
    private String cardLastFour;

    public Card() {
    }

    public Card(String paymentId, String invoiceId, double amount,
                       PaymentType paymentType, LocalDate paymentDate,
                       String cardHolderName, String cardLastFour) {
        super(paymentId, invoiceId, amount, paymentType, paymentDate);
        setCardHolderName(cardHolderName);
        setCardLastFour(cardLastFour);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    protected void validateMethodRules(List<String> errors) {
        if (cardHolderName == null || !cardHolderName.trim().matches("^[a-zA-Z ]{3,100}$")) {
            errors.add("Card holder name must be 3 to 100 letters");
        }
        if (cardLastFour == null || !cardLastFour.trim().matches("^[0-9]{4}$")) {
            errors.add("Enter the last 4 digits of the card");
        }
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }
}
