package com.hotel.user.staff;

import com.hotel.user.User;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Staff extends User {

    private StaffType staffType;
    private String position;
    private double salary;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joinedDate;
    private String username;
    private String password;
    private Status status;

    public Staff() {
        super();
    }

    public Staff(String userId, String name, String nicOrPassport, String contactNo, String email, String address,
                 StaffType staffType, String position, double salary, LocalDate joinedDate,
                 String username, String password, Status status) {
        super(userId, name, nicOrPassport, contactNo, email, address);
        this.staffType = staffType;
        this.position = position;
        this.salary = salary;
        this.joinedDate = joinedDate;
        this.username = username;
        this.password = password;
        this.status = status;
    }

    @Override
    public String getRole() {
        return staffType != null ? staffType.name() : "";
    }

    public boolean canLogin() {
        return (staffType == StaffType.ADMIN || staffType == StaffType.RECEPTIONIST)
                && status == Status.ACTIVE;
    }

    /**
     * Validates staff attributes according to specification rules.
     * Returns a list of error messages.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Name: Required, 3 to 100 letters and spaces
        if (name == null || name.trim().isEmpty()) {
            errors.add("Name is required");
        } else if (!name.matches("^[a-zA-Z ]{3,100}$")) {
            errors.add("Name must be 3 to 100 letters and spaces only");
        }

        // NIC: Required and unique. Staff: NIC (9 digits + V/X, or 12 digits)
        if (nicOrPassport == null || nicOrPassport.trim().isEmpty()) {
            errors.add("NIC is required");
        } else if (!nicOrPassport.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            errors.add("NIC must be 9 digits followed by V/X or 12 digits");
        }

        // ContactNo: Required. Staff: 10 digits starting with 0
        if (contactNo == null || contactNo.trim().isEmpty()) {
            errors.add("Contact number is required");
        } else if (!contactNo.matches("^0[0-9]{9}$")) {
            errors.add("Contact number must be 10 digits starting with 0");
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

        // StaffType: Required, must be ADMIN, RECEPTIONIST or EMPLOYEE
        if (staffType == null) {
            errors.add("Staff type is required (ADMIN, RECEPTIONIST or EMPLOYEE)");
        }

        // Position: Required
        if (position == null || position.trim().isEmpty()) {
            errors.add("Position is required");
        }

        // Salary: Required, greater than 0
        if (salary <= 0) {
            errors.add("Salary is required and must be greater than 0");
        }

        // JoinedDate: Required, can't be a future date
        if (joinedDate == null) {
            errors.add("Joined date is required");
        } else if (joinedDate.isAfter(LocalDate.now())) {
            errors.add("Joined date cannot be a future date");
        }

        // Username rules
        if (staffType == StaffType.ADMIN || staffType == StaffType.RECEPTIONIST) {
            if (username == null || username.trim().isEmpty()) {
                errors.add("Username is required for ADMIN and RECEPTIONIST");
            } else if (!username.matches("^[a-zA-Z0-9._]{4,20}$")) {
                errors.add("Username must be 4 to 20 letters, digits, dots or underscores");
            }
        } else if (staffType == StaffType.EMPLOYEE) {
            if (username != null && !username.trim().isEmpty()) {
                errors.add("EMPLOYEE username must be empty");
            }
        }

        // Password rules
        if (staffType == StaffType.ADMIN || staffType == StaffType.RECEPTIONIST) {
            if (password != null && !password.isEmpty()) {
                if (password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
                    errors.add("Password must be at least 8 characters with at least one letter and one number");
                }
            }
        } else if (staffType == StaffType.EMPLOYEE) {
            if (password != null && !password.trim().isEmpty()) {
                errors.add("EMPLOYEE password must be empty");
            }
        }

        // Status: ACTIVE or INACTIVE
        if (status == null) {
            errors.add("Status is required (ACTIVE or INACTIVE)");
        }

        return errors;
    }

    public StaffType getStaffType() {
        return staffType;
    }

    public void setStaffType(StaffType staffType) {
        this.staffType = staffType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDate joinedDate) {
        this.joinedDate = joinedDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
