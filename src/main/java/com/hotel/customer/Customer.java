package com.hotel.customer;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Customer {

    protected String customerId;
    protected String customerName;
    protected String nicOrPassport;
    protected String contactNo;
    protected String email;
    protected String address;
    protected String nationality;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    protected LocalDate dateOfBirth;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    protected LocalDate registeredDate;
    protected double discount;

    public Customer() {
    }

    public Customer(String customerId, String customerName, String nicOrPassport, String contactNo,
                    String email, String address, String nationality,
                    LocalDate dateOfBirth, LocalDate registeredDate, double discount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.nicOrPassport = nicOrPassport;
        this.contactNo = contactNo;
        this.email = email;
        this.address = address;
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
        this.registeredDate = registeredDate;
        this.discount = discount;
    }

    /** Returns "LOCAL" or "FOREIGN". */
    public abstract String getCustomerType();

    /** Each subclass checks its own NIC or passport, contact number and nationality. */
    protected abstract void validateTypeRules(List<String> errors);

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Name: Required, 3 to 100 letters and spaces
        if (customerName == null || customerName.trim().isEmpty()) {
            errors.add("Name is required");
        } else if (!customerName.matches("^[a-zA-Z ]{3,100}$")) {
            errors.add("Name must be 3 to 100 letters and spaces only");
        }

        // Email: Optional. If given, must be a valid email format
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errors.add("Email must be in a valid format");
            }
        }

        // Address: Optional, max 255 characters
        if (address != null && address.length() > 255) {
            errors.add("Address cannot exceed 255 characters");
        }

        // DateOfBirth: Optional. If given, must be in the past and the customer must be 18 or older
        if (dateOfBirth != null) {
            if (!dateOfBirth.isBefore(LocalDate.now())) {
                errors.add("Date of birth must be in the past");
            } else if (dateOfBirth.plusYears(18).isAfter(LocalDate.now())) {
                errors.add("Customer must be 18 or older");
            }
        }

        // RegisteredDate: can't be a future date
        if (registeredDate != null && registeredDate.isAfter(LocalDate.now())) {
            errors.add("Registered date cannot be a future date");
        }

        // Discount: percentage from 0 to 100
        if (discount < 0 || discount > 100) {
            errors.add("Discount must be between 0 and 100 percent");
        }

        // NIC or passport, contact number and nationality depend on the customer type
        validateTypeRules(errors);

        return errors;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }
}
