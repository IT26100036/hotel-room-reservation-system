package com.hotel.reservation.booking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        return "booking/list";
    }
}
