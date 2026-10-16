package com.hotel.reservation.booking;

public class OptionItem {
    private final int id;
    private final String label;
    private final double rate;   // room rate per night; 0 for guests

    public OptionItem(int id, String label) {
        this(id, label, 0);
    }

    public OptionItem(int id, String label, double rate) {
        this.id = id;
        this.label = label;
        this.rate = rate;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getRate() {
        return rate;
    }
}
