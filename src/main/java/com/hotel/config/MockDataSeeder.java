package com.hotel.config;

import com.hotel.common.ValidationException;
import com.hotel.customer.Customer;
import com.hotel.customer.CustomerService;
import com.hotel.customer.ForeignCustomer;
import com.hotel.customer.LocalCustomer;
import com.hotel.finance.Invoice;
import com.hotel.finance.Payment;
import com.hotel.finance.enums.PaymentType;
import com.hotel.finance.methods.Card;
import com.hotel.finance.methods.Cash;
import com.hotel.finance.methods.Online;
import com.hotel.finance.services.InvoiceService;
import com.hotel.finance.services.PaymentService;
import com.hotel.hotelservice.ServiceRequest;
import com.hotel.hotelservice.ServiceRequestService;
import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.TripType;
import com.hotel.hotelservice.enums.VehicleType;
import com.hotel.hotelservice.types.FoodRequest;
import com.hotel.hotelservice.types.LaundryRequest;
import com.hotel.hotelservice.types.TransportRequest;
import com.hotel.room.DoubleRoom;
import com.hotel.room.Room;
import com.hotel.room.RoomService;
import com.hotel.room.RoomStatus;
import com.hotel.room.SingleRoom;
import com.hotel.room.SuiteRoom;
import com.hotel.user.staff.Staff;
import com.hotel.user.staff.StaffService;
import com.hotel.user.staff.StaffType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Fills the app with sample data so every page has something to show.
 *
 * - Runs after DataLoader (which creates the admin S001).
 * - Each part is filled only when its data file is still empty, so it never adds duplicates
 *   and never touches data you entered yourself.
 * - Everything is created through the normal services, so all validation rules, IDs and
 *   history logs work exactly as if staff typed it in.
 * - Turn it off with app.seed-mock-data=false in application.properties.
 */
@Component
@Order(2)
public class MockDataSeeder implements CommandLineRunner {

    private final StaffService staffService;
    private final CustomerService customerService;
    private final RoomService roomService;
    private final ServiceRequestService serviceRequestService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final boolean enabled;

    public MockDataSeeder(StaffService staffService,
                          CustomerService customerService,
                          RoomService roomService,
                          ServiceRequestService serviceRequestService,
                          InvoiceService invoiceService,
                          PaymentService paymentService,
                          @Value("${app.seed-mock-data:true}") boolean enabled) {
        this.staffService = staffService;
        this.customerService = customerService;
        this.roomService = roomService;
        this.serviceRequestService = serviceRequestService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        seedStaff();
        seedCustomers();
        seedRooms();
        seedServiceRequests();
        seedInvoicesAndPayments();
    }

    /* ---------------- Staff ---------------- */

    private void seedStaff() {
        if (staffService.getAll().size() > 1) {
            return;   // more than the admin already exists
        }
        LocalDate today = LocalDate.now();
        addStaff("Kasun Silva", "199512345678", "0712233445", "kasun@hotel.lk", "Kandy",
                StaffType.RECEPTIONIST, "Front Desk", 65000, today.minusYears(2), "kasun", "kasun1234");
        addStaff("Ruwan Bandara", "199234567890", "0763344556", null, "Galle",
                StaffType.RECEPTIONIST, "Night Receptionist", 60000, today.minusMonths(8), "ruwan", "ruwan1234");
        addStaff("Dilini Jayasuriya", "199845678901", "0759876543", null, "Colombo",
                StaffType.EMPLOYEE, "Housekeeping", 45000, today.minusYears(1), null, null);
        addStaff("Nuwan Perera", "199056789012", "0778899001", null, "Negombo",
                StaffType.EMPLOYEE, "Driver", 50000, today.minusMonths(5), null, null);
        System.out.println("MockDataSeeder: sample staff added (receptionist logins: kasun / kasun1234, ruwan / ruwan1234)");
    }

    private void addStaff(String name, String nic, String contact, String email, String address,
                          StaffType type, String position, double salary, LocalDate joined,
                          String username, String password) {
        Staff s = new Staff();
        s.setName(name);
        s.setNicOrPassport(nic);
        s.setContactNo(contact);
        s.setEmail(email);
        s.setAddress(address);
        s.setStaffType(type);
        s.setPosition(position);
        s.setSalary(salary);
        s.setJoinedDate(joined);
        s.setUsername(username);
        s.setPassword(password);
        save("staff " + name, () -> staffService.create(s));
    }

    /* ---------------- Customers ---------------- */

    private void seedCustomers() {
        if (!customerService.getAll().isEmpty()) {
            return;
        }
        addLocal("Sunil Fernando", "198812345678", "0712345678", "sunil@gmail.com", "Kandy",
                LocalDate.of(1988, 4, 4), 0);                                         // C001
        addLocal("Nimali Perera", "199812345678", "0771234567", "nimali@gmail.com", "Colombo 05",
                LocalDate.of(1998, 9, 12), 5);                                        // C002
        addForeign("James Carter", "N1234567", "+447911123456", "james@mail.com", "12 Baker Street, London",
                "British", LocalDate.of(1990, 4, 12), 0);                             // C003
        addForeign("Aiko Tanaka", "TK8823410", "+819012345678", null, "Osaka",
                "Japanese", LocalDate.of(1985, 1, 20), 10);                           // C004
        addLocal("Chamara Wijesinghe", "200012345678", "0705566778", null, "Matara",
                LocalDate.of(2000, 7, 30), 0);                                        // C005
        System.out.println("MockDataSeeder: sample customers added");
    }

    private void addLocal(String name, String nic, String contact, String email, String address,
                          LocalDate dob, double discount) {
        LocalCustomer c = new LocalCustomer();
        fillCustomer(c, name, nic, contact, email, address, dob, discount);
        save("customer " + name, () -> customerService.register(c));
    }

    private void addForeign(String name, String passport, String contact, String email, String address,
                            String nationality, LocalDate dob, double discount) {
        ForeignCustomer c = new ForeignCustomer();
        fillCustomer(c, name, passport, contact, email, address, dob, discount);
        c.setNationality(nationality);
        save("customer " + name, () -> customerService.register(c));
    }

    private void fillCustomer(Customer c, String name, String nicOrPassport, String contact, String email,
                              String address, LocalDate dob, double discount) {
        c.setCustomerName(name);
        c.setNicOrPassport(nicOrPassport);
        c.setContactNo(contact);
        c.setEmail(email);
        c.setAddress(address);
        c.setDateOfBirth(dob);
        c.setDiscount(discount);
    }

    /* ---------------- Rooms ---------------- */

    private void seedRooms() {
        if (!roomService.getAll().isEmpty()) {
            return;
        }
        addRoom(new SingleRoom(), "101", 1, 1, 8000, true, "Garden view");          // R001
        addRoom(new SingleRoom(), "102", 1, 1, 7000, false, null);                  // R002
        addRoom(new DoubleRoom(), "201", 2, 2, 12500, true, "Pool view");           // R003
        addRoom(new DoubleRoom(), "202", 2, 2, 11000, false, "Twin beds");          // R004
        addRoom(new SuiteRoom(), "301", 3, 4, 20000, true, "Sea view with balcony"); // R005
        addRoom(new SuiteRoom(), "302", 3, 3, 22000, true, "Family suite");         // R006

        // A few rooms in other states, so the status colours show
        save("room status R003", () -> roomService.changeStatus("R003", RoomStatus.OCCUPIED));
        save("room status R004", () -> roomService.changeStatus("R004", RoomStatus.CLEANING));
        save("room status R006", () -> roomService.changeStatus("R006", RoomStatus.MAINTENANCE));
        System.out.println("MockDataSeeder: sample rooms added");
    }

    private void addRoom(Room room, String number, int floor, int capacity, double price,
                         boolean ac, String description) {
        room.setRoomNumber(number);
        room.setFloor(floor);
        room.setCapacity(capacity);
        room.setPricePerNight(price);
        room.setHasAirConditioning(ac);
        room.setDescription(description);
        save("room " + number, () -> roomService.create(room));
    }

    /* ---------------- Hotel services ---------------- */

    private void seedServiceRequests() {
        if (!serviceRequestService.getAll().isEmpty()) {
            return;
        }
        FoodRequest food = new FoodRequest();
        food.setCustomerId("C001");
        food.setItemName("Fried rice");
        food.setQuantity(2);
        food.setUnitPrice(1200);
        food.setNotes("Not spicy");
        ServiceRequest sr1 = save("food request", () -> serviceRequestService.create(food));   // SR001

        LaundryRequest laundry = new LaundryRequest();
        laundry.setCustomerId("C003");
        laundry.setItemCount(6);
        laundry.setExpress(true);
        save("laundry request", () -> serviceRequestService.create(laundry));                 // SR002

        TransportRequest pickup = new TransportRequest();
        pickup.setCustomerId("C004");
        pickup.setTripType(TripType.PICKUP);
        pickup.setVehicleType(VehicleType.VAN);
        pickup.setLocation("Bandaranaike Airport");
        pickup.setDistanceKm(30);
        pickup.setTravelDate(LocalDate.now().plusDays(1));
        save("transport request", () -> serviceRequestService.create(pickup));                // SR003

        FoodRequest drinks = new FoodRequest();
        drinks.setCustomerId("C002");
        drinks.setItemName("Fresh lime juice");
        drinks.setQuantity(3);
        drinks.setUnitPrice(450);
        save("food request", () -> serviceRequestService.create(drinks));                     // SR004

        // Move some along, so every status shows on the list
        if (sr1 != null) {
            save("status SR001", () -> serviceRequestService.changeStatus("SR001", RequestStatus.IN_PROGRESS));
            save("status SR001", () -> serviceRequestService.changeStatus("SR001", RequestStatus.COMPLETED));
        }
        save("status SR002", () -> serviceRequestService.changeStatus("SR002", RequestStatus.IN_PROGRESS));
        save("status SR004", () -> serviceRequestService.changeStatus("SR004", RequestStatus.CANCELLED));
        System.out.println("MockDataSeeder: sample service requests added");
    }

    /* ---------------- Finance ---------------- */

    private void seedInvoicesAndPayments() {
        if (!invoiceService.getAll().isEmpty()) {
            return;
        }
        // Booking IDs are examples until Booking Management is added
        addInvoice("B001", "C001", 16000, 2400);   // INV001: will be fully paid
        addInvoice("B002", "C002", 12500, 0);      // INV002: advance only (C002 has 5% discount)
        addInvoice("B003", "C003", 44000, 2250);   // INV003: not paid yet
        addInvoice("B004", "C004", 8000, 0);       // INV004: voided

        // INV001: cash advance, then the rest by card -> PAID
        Cash advance = new Cash();
        advance.setInvoiceId("INV001");
        advance.setPaymentType(PaymentType.ADVANCE);
        advance.setAmount(5000);
        advance.setAmountReceived(5000);
        save("payment", () -> paymentService.record(advance));

        Card rest = new Card();
        rest.setInvoiceId("INV001");
        rest.setPaymentType(PaymentType.FINAL);
        rest.setAmount(13400);
        rest.setCardHolderName("Sunil Fernando");
        rest.setCardLastFour("4821");
        save("payment", () -> paymentService.record(rest));

        // INV002: online advance -> PARTIALLY_PAID
        Online online = new Online();
        online.setInvoiceId("INV002");
        online.setPaymentType(PaymentType.ADVANCE);
        online.setAmount(3000);
        online.setReferenceNo("TRX88213409");
        save("payment", () -> paymentService.record(online));

        // INV004: a mistake that was voided
        save("void INV004", () -> invoiceService.voidInvoice("INV004"));
        System.out.println("MockDataSeeder: sample invoices and payments added");
    }

    private void addInvoice(String bookingId, String customerId, double roomCharge, double extraCharges) {
        Invoice invoice = new Invoice();
        invoice.setBookingId(bookingId);
        invoice.setCustomerId(customerId);
        invoice.setRoomCharge(roomCharge);
        invoice.setExtraCharges(extraCharges);
        save("invoice for " + bookingId, () -> invoiceService.create(invoice));
    }

    /* ---------------- Helper ---------------- */

    /** Something that creates or changes one record */
    private interface Step<T> {
        T run();
    }

    /** Runs one step; if a rule fails it prints why and carries on, so the app always starts */
    private <T> T save(String what, Step<T> step) {
        try {
            return step.run();
        } catch (ValidationException e) {
            System.err.println("MockDataSeeder: skipped " + what + " - " + e.getErrors());
            return null;
        }
    }

    /** Same, for steps that return nothing */
    private void save(String what, Runnable step) {
        try {
            step.run();
        } catch (ValidationException e) {
            System.err.println("MockDataSeeder: skipped " + what + " - " + e.getErrors());
        }
    }
}
