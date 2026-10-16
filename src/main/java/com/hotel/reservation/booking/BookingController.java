package com.hotel.reservation.booking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Web pages for bookings

@Controller
@RequestMapping("/booking")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // GET /booking/list
    // GET /booking/list?status=CHECKED_IN
    @GetMapping("/list")
    public String list(@RequestParam(required = false) BookingStatus status, Model model) {
        if (status == null)
            model.addAttribute("bookings", bookingService.getAllBookings());
        else
            model.addAttribute("bookings", bookingService.getBookingsByStatus(status));

        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", BookingStatus.values());

        List<Booking> all = bookingService.getAllBookings();
        Map<BookingStatus, Integer> counts = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values())
            counts.put(s, 0);
        for (Booking b : all)
            counts.put(b.getStatus(), counts.get(b.getStatus()) + 1);
        model.addAttribute("counts", counts);
        model.addAttribute("totalCount", all.size());
        return "booking/list";
    }

    // GET /booking/5
    // Shows one booking in full; goes back to the list if the id does not exist
    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("booking", bookingService.getBooking(id));
            model.addAttribute("today", LocalDate.now());
            return "booking/view";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/list";
        }
    }

    // ---------- row actions (buttons on the list page) ----------
    // Each one changes the status, then goes back to the list with a message
    // If the change is not allowed, the error from Booking is shown instead

    // POST /booking/5/check-in
    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable int id, RedirectAttributes redirect) {
        try {
            bookingService.checkIn(id);
            redirect.addFlashAttribute("success", "B-" + id + " checked in");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/list";
    }

    // POST /booking/5/check-out
    @PostMapping("/{id}/check-out")
    public String checkOut(@PathVariable int id, RedirectAttributes redirect) {
        try {
            bookingService.checkOut(id);
            redirect.addFlashAttribute("success", "B-" + id + " checked out");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/list";
    }

    // POST /booking/5/cancel
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable int id, RedirectAttributes redirect) {
        try {
            bookingService.cancel(id);
            redirect.addFlashAttribute("success", "B-" + id + " cancelled");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/list";
    }
}
