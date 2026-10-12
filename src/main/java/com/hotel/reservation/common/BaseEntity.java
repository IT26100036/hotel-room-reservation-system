package com.hotel.reservation.common;

public abstract class BaseEntity {
    private int id;

    protected BaseEntity(){
    }

    protected BaseEntity(int id){
        setId(id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0)
            throw new IllegalArgumentException("Id cannot be negative");
        this.id = id;
    }

    public abstract String getDisplayName();
}
