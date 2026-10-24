package com.hotel.customer;

import java.time.LocalDate;
import java.util.List;

public class LocalCustomer extends Customer {

    public static final String LOCAL_NATIONALITY = "Sri Lankan";

    public LocalCustomer() {
        super();
        this.nationality = LOCAL_NATIONALITY;
    }

    public LocalCustomer(String customerId, String customerName, String nicOrPassport, String contactNo,
                         String email, String address, LocalDate dateOfBirth,
                         LocalDate registeredDate, double discount) {
        super(customerId, customerName, nicOrPassport, contactNo, email, address,
                LOCAL_NATIONALITY, dateOfBirth, registeredDate, discount);
    }

    @Override
    public String getCustomerType() {
        return "LOCAL";
    }

    /** A local customer is always Sri Lankan, whatever the form sends. */
    @Override
    public void setNationality(String nationality) {
        this.nationality = LOCAL_NATIONALITY;
    }

    @Override
    protected void validateTypeRules(List<String> errors) {
        // NIC: Required. 9 digits + V/X, or 12 digits
        if (nicOrPassport == null || nicOrPassport.trim().isEmpty()) {
            errors.add("NIC is required");
        } else if (!nicOrPassport.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            errors.add("NIC must be 9 digits followed by V/X or 12 digits");
        }

        // ContactNo: Required. 10 digits starting with 0
        if (contactNo == null || contactNo.trim().isEmpty()) {
            errors.add("Contact number is required");
        } else if (!contactNo.matches("^0[0-9]{9}$")) {
            errors.add("Contact number must be 10 digits starting with 0");
        }
    }
}
