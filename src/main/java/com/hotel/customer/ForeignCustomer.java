package com.hotel.customer;

import java.time.LocalDate;
import java.util.List;

public class ForeignCustomer extends Customer {

    public ForeignCustomer() {
        super();
    }

    public ForeignCustomer(String customerId, String customerName, String nicOrPassport, String contactNo,
                           String email, String address, String nationality, LocalDate dateOfBirth,
                           LocalDate registeredDate, double discount) {
        super(customerId, customerName, nicOrPassport, contactNo, email, address,
                nationality, dateOfBirth, registeredDate, discount);
    }

    @Override
    public String getCustomerType() {
        return "FOREIGN";
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        // Passport: Required. 6 to 9 letters or digits
        if (nicOrPassport == null || nicOrPassport.trim().isEmpty()) {
            errors.add("Passport number is required");
        } else if (!nicOrPassport.matches("^[A-Za-z0-9]{6,9}$")) {
            errors.add("Passport number must be 6 to 9 letters or digits");
        }

        // ContactNo: Required. Optional + then 7 to 15 digits
        if (contactNo == null || contactNo.trim().isEmpty()) {
            errors.add("Contact number is required");
        } else if (!contactNo.matches("^\\+?[0-9]{7,15}$")) {
            errors.add("Contact number must be 7 to 15 digits, optionally starting with +");
        }

        // Nationality: Required for foreign customers
        if (nationality == null || nationality.trim().isEmpty()) {
            errors.add("Nationality is required");
        } else if (!nationality.matches("^[a-zA-Z ]{2,50}$")) {
            errors.add("Nationality must be 2 to 50 letters");
        }
    }
}
