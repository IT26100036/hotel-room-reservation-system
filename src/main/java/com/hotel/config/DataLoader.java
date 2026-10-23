package com.hotel.config;

import com.hotel.user.staff.Staff;
import com.hotel.user.staff.StaffRepository;
import com.hotel.user.staff.StaffType;
import com.hotel.user.staff.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {

    private final StaffRepository staffRepository;

    @Autowired
    public DataLoader(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public void run(String... args) {
        // If no ADMIN exists, create S001 with username "admin" and password "admin1234" in staff text file
        boolean hasAdmin = staffRepository.findAll().stream()
                .anyMatch(staff -> staff.getStaffType() == StaffType.ADMIN);

        if (!hasAdmin) {
            Staff firstAdmin = new Staff();
            firstAdmin.setUserId("S001");
            firstAdmin.setName("System Administrator");
            firstAdmin.setNicOrPassport("199012345678");
            firstAdmin.setContactNo("0771234567");
            firstAdmin.setEmail("admin@hotel.com");
            firstAdmin.setAddress("Hotel Head Office");
            firstAdmin.setStaffType(StaffType.ADMIN);
            firstAdmin.setPosition("System Administrator");
            firstAdmin.setSalary(100000.00);
            firstAdmin.setJoinedDate(LocalDate.now());
            firstAdmin.setUsername("admin");
            firstAdmin.setPassword("admin1234");
            firstAdmin.setStatus(Status.ACTIVE);

            staffRepository.save(firstAdmin);
            System.out.println("DataLoader: Initial ADMIN created in staff text file with username 'admin' and password 'admin1234'");
        }
    }
}
