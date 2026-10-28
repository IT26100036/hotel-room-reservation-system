package com.hotel.booking;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.customer.CustomerService;
import com.hotel.room.Room;
import com.hotel.room.RoomService;
import com.hotel.room.RoomStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final RoomService roomService;

    public BookingController(BookingService bookingService, CustomerService customerService, RoomService roomService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.roomService = roomService;
    }

    /**
     * Booking is abstract, so Spring can't create one from the form by itself.
     * This creates a WalkInBooking or a ReservationBooking from the "bookingType" value,
     * then Spring fills in the form fields on that object.
     */
    @ModelAttribute("booking")
    public Booking bookingFromType(@RequestParam(value = "bookingType", required = false) String bookingType) {
        if ("RESERVATION".equalsIgnoreCase(bookingType)) {
            return new ReservationBooking();
        }
        return new WalkInBooking();
    }

    /* ---------- List and details ---------- */

    @GetMapping
    public String listBookings(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", required = false) BookingStatus status,
                               Model model) {
        List<Booking> bookings = (status != null) ? bookingService.getByStatus(status) : bookingService.search(keyword);
        model.addAttribute("bookingList", bookings);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/list";
    }

    @GetMapping("/view/{id}")
    public String viewBooking(@PathVariable("id") String id, Model model) {
        Booking booking = bookingService.getById(id);
        model.addAttribute("booking", booking);
        model.addAttribute("roomTotal", bookingService.getRoomTotal(booking));
        model.addAttribute("requiredAdvance", bookingService.getRequiredAdvance(booking));
        try {
            model.addAttribute("room", roomService.getById(booking.getRoomId()));
        } catch (NotFoundException ignored) {
        }
        try {
            model.addAttribute("customer", customerService.getById(booking.getCustomerId()));
        } catch (NotFoundException ignored) {
        }
        return "booking/details";
    }

    /* ---------- Create ---------- */

    @GetMapping("/create")
    public String showCreateForm(@ModelAttribute("booking") Booking booking, Model model) {
        // sensible starting values: check in today, one night, one guest
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckOutDate(LocalDate.now().plusDays(1));
        booking.setNumberOfGuests(1);
        addFormOptions(model, false);
        return "booking/form";
    }

    @PostMapping("/create")
    public String createBooking(@ModelAttribute("booking") Booking booking, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Booking saved = bookingService.create(booking);
            redirectAttributes.addFlashAttribute("successMessage", "Booking " + saved.getBookingId() + " created successfully!");
            return "redirect:/booking/view/" + saved.getBookingId();
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            addFormOptions(model, false);
            return "booking/form";
        }
    }

    /* ---------- Update ---------- */

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("booking", bookingService.getById(id));
        addFormOptions(model, true);
        return "booking/form";
    }

    @PostMapping("/edit/{id}")
    public String updateBooking(@PathVariable("id") String id, @ModelAttribute("booking") Booking form,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            bookingService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Booking updated successfully!");
            return "redirect:/booking/view/" + id;
        } catch (ValidationException e) {
            Booking existing = bookingService.getById(id);
            form.setBookingId(existing.getBookingId());
            form.setCustomerId(existing.getCustomerId());
            form.setStatus(existing.getStatus());
            model.addAttribute("booking", form);
            model.addAttribute("errors", e.getErrors());
            addFormOptions(model, true);
            return "booking/form";
        }
    }

    /* ---------- Status actions ---------- */

    @PostMapping("/confirm/{id}")
    public String confirm(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        return runAction(id, "Booking confirmed.", () -> bookingService.confirm(id), redirectAttributes);
    }

    @PostMapping("/checkin/{id}")
    public String checkIn(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        return runAction(id, "Guest checked in. Room is now OCCUPIED.", () -> bookingService.checkIn(id), redirectAttributes);
    }

    @PostMapping("/checkout/{id}")
    public String checkOut(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        return runAction(id, "Guest checked out. Room is now CLEANING.", () -> bookingService.checkOut(id), redirectAttributes);
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        return runAction(id, "Booking cancelled.", () -> bookingService.cancel(id), redirectAttributes);
    }

    /** Runs one status change and shows a success or error message on the details page */
    private String runAction(String id, String successMessage, Runnable action, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/booking/view/" + id;
    }

    /* ---------- Delete ---------- */

    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking " + id + " deleted.");
            return "redirect:/booking";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/view/" + id;
        }
    }

    /* ---------- History ---------- */

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("logs", bookingService.readLog());
        return "booking/history";
    }

    /** Lists for the form's drop-downs (rooms under maintenance are left out) */
    private void addFormOptions(Model model, boolean isEdit) {
        List<Room> rooms = roomService.getAll().stream()
                .filter(r -> r.getStatus() != RoomStatus.MAINTENANCE)
                .collect(Collectors.toList());
        model.addAttribute("rooms", rooms);
        model.addAttribute("customers", customerService.getAll());
        model.addAttribute("isEdit", isEdit);
    }
}
