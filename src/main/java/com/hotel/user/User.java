package com.hotel.user;

public class User {
    private String userId;
    private String name;
    private String nicOrPassport;
    private String contactNo;
    private String email;
    private String address;

    public User(){
    }

    public User(String userId, String name, String nicOrPassport,
                String contactNo, String email, String address) {
        this.userId = userId;
        this.name = name;
        this.nicOrPassport = nicOrPassport;
        this.contactNo = contactNo;
        this.email = email;
        this.address = address;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNicOrPassport() {
        return nicOrPassport;
    }

    public void setNicOrPassport(String nicOrPassport) {
        this.nicOrPassport = nicOrPassport;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public abstract String getRole();


}
