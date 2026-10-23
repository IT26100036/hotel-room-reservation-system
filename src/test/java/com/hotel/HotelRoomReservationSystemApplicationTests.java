package com.hotel;

import com.hotel.user.staff.Staff;
import com.hotel.user.staff.StaffRepository;
import com.hotel.user.staff.StaffService;
import com.hotel.user.staff.StaffType;
import com.hotel.user.staff.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HotelRoomReservationSystemApplicationTests {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Clean up any previously created test receptionist to allow repeatable tests
        staffRepository.findByUsername("test.rec").ifPresent(s -> staffRepository.delete(s));
    }

    @Test
    void testStaffFormRendering() throws Exception {
        Staff admin = staffService.getById("S001");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInStaff", admin);

        // Test GET /staff/create form rendering
        mockMvc.perform(get("/staff/create").session(session))
                .andExpect(status().isOk());

        // Test GET /staff/edit/{id} form rendering
        mockMvc.perform(get("/staff/edit/S001").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void testInitialAdminCreatedByDataLoader() {
        List<Staff> staffList = staffService.getAll();
        assertFalse(staffList.isEmpty(), "DataLoader should have created at least one admin in text file");

        Staff admin = staffService.getById("S001");
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
        assertEquals(StaffType.ADMIN, admin.getStaffType());
        assertEquals(Status.ACTIVE, admin.getStatus());
        assertTrue(admin.canLogin());
    }

    @Test
    void testStaffIdGenerationAndValidation() {
        Staff staff = new Staff();
        staff.setName("Test Receptionist");
        staff.setNicOrPassport("199512345678");
        staff.setContactNo("0712345678");
        staff.setEmail("rec@hotel.com");
        staff.setAddress("Colombo");
        staff.setStaffType(StaffType.RECEPTIONIST);
        staff.setPosition("Front Desk Officer");
        staff.setSalary(75000.0);
        staff.setJoinedDate(LocalDate.now());
        staff.setUsername("test.rec");
        staff.setPassword("pass1234");
        staff.setStatus(Status.ACTIVE);

        Staff created = staffService.create(staff);
        assertNotNull(created.getUserId());
        assertTrue(created.getUserId().startsWith("S"));
        assertEquals("RECEPTIONIST", created.getRole());

        // Test staff login
        Staff loggedIn = staffService.login("test.rec", "pass1234");
        assertNotNull(loggedIn);
        assertEquals(created.getUserId(), loggedIn.getUserId());

        // Verify that the file can be read with a BufferedReader directly
        File file = new File("data/test_staff.txt");
        if (!file.exists()) {
            file = new File("data/staff.txt");
        }
        assertTrue(file.exists());
        boolean foundInFile = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("test.rec")) {
                    foundInFile = true;
                    break;
                }
            }
        } catch (Exception e) {
            fail("Failed to read staff file with BufferedReader: " + e.getMessage());
        }
        assertTrue(foundInFile, "Created staff record should exist in the text file");
    }

    @Test
    void testStaffLogHistoryWithBufferedReaderWriter() {
        List<String> logs = staffService.readLog();
        assertNotNull(logs);
        assertFalse(logs.isEmpty(), "Staff log file should contain log entries");
    }
}
