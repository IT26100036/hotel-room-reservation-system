package com.hotel.reservation.booking;

import org.springframework.format.annotation.DateTimeFormat;
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
    // GET /booking/list?walkIn=true  (opens the walk-in window straight away)
    @GetMapping("/list")
    public String list(@RequestParam(required = false) BookingStatus status,
                       @RequestParam(required = false) boolean walkIn, Model model) {
        prepareList(model, status);
        model.addAttribute("openWalkIn", walkIn);
        return "booking/list";
    }

    // Everything list.jsp needs: the bookings, the filter with counts,
    // and the guest and room choices for the walk-in window
    private void prepareList(Model model, BookingStatus status) {
        if (status == null)
            model.addAttribute("bookings", bookingService.getAllBookings());
        else
            model.addAttribute("bookings", bookingService.getBookingsByStatus(status));

        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", BookingStatus.values());

        // Numbers on the filter chips: "All 8", "Confirmed 3", ...
        List<Booking> all = bookingService.getAllBookings();
        Map<BookingStatus, Integer> counts = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values())
            counts.put(s, 0);
        for (Booking b : all)
            counts.put(b.getStatus(), counts.get(b.getStatus()) + 1);
        model.addAttribute("counts", counts);
        model.addAttribute("totalCount", all.size());

        // Walk-in window: stay starts today, rooms free tonight
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("tomorrow", today.plusDays(1));
        model.addAttribute("guests", bookingService.getGuestOptions());
        model.addAttribute("availableRooms", bookingService.getAvailableRoomOptions(today, today.plusDays(1)));
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

    // ---------- walk-in window (on the list page) ----------

    // GET /booking/new - old link: open the list with the walk-in window showing
    @GetMapping("/new")
    public String newWalkIn() {
        return "redirect:/booking/list?walkIn=true";
    }

    // POST /booking/walk-in - "Book & check in"
    // guestChoice is a guest id, or "new" when the guest's details are typed in
    @PostMapping("/walk-in")
    public String walkIn(@RequestParam(required = false) String guestChoice,
                         @RequestParam(required = false) String newName,
                         @RequestParam(required = false) String newNic,
                         @RequestParam(required = false) String newContact,
                         @RequestParam(required = false) String newEmail,
                         @RequestParam(required = false) Integer roomId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                         Model model, RedirectAttributes redirect) {
        try {
            int guestId;
            if ("new".equals(guestChoice))
                guestId = 0;   // the service saves the new guest first
            else if (guestChoice == null || guestChoice.isBlank())
                throw new IllegalArgumentException("Choose a guest or add a new one");
            else {
                Integer chosen = parseIdOrNull(guestChoice);
                guestId = chosen == null ? -1 : chosen;   // -1 is rejected as "A valid guest is required"
            }

            int newId = bookingService.walkInAndCheckIn(guestId, newName, newNic, newContact, newEmail,
                    toInt(roomId), dateTo);
            redirect.addFlashAttribute("success", "Booking B-" + newId + " created and checked in");
            return "redirect:/booking/" + newId;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Show the list again with the window open, the message and the values typed in
            prepareList(model, null);
            model.addAttribute("openWalkIn", true);
            model.addAttribute("walkInError", e.getMessage());
            // The chosen guest id, or null for "+ New guest"
            model.addAttribute("guestChoiceId", parseIdOrNull(guestChoice));
            model.addAttribute("newName", newName);
            model.addAttribute("newNic", newNic);
            model.addAttribute("newContact", newContact);
            model.addAttribute("newEmail", newEmail);
            model.addAttribute("roomId", roomId);
            model.addAttribute("dateTo", dateTo);
            return "booking/list";
        }
    }

    // ---------- form page: edit and extend ----------
    // One page (booking/form.jsp) used two ways, chosen by "mode":
    //   edit   - confirmed booking: change room and dates (guest is locked)
    //   extend - checked-in booking: only the check-out date can move later
    // On an error the form is shown again with the message and the values typed in.

    // GET /booking/5/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model model, RedirectAttributes redirect) {
        return openFilledForm(id, "edit", model, redirect);
    }

    // POST /booking/5/edit
    @PostMapping("/{id}/edit")
    public String update(@PathVariable int id,
                         @RequestParam(required = false) Integer guestId,
                         @RequestParam(required = false) Integer roomId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                         Model model, RedirectAttributes redirect) {
        try {
            bookingService.updateBooking(id, toInt(roomId), dateFrom, dateTo);
            redirect.addFlashAttribute("success", "Booking B-" + id + " updated");
            return "redirect:/booking/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            prepareForm(model, "edit", id, guestId, roomId, dateFrom, dateTo);
            return "booking/form";
        }
    }

    // GET /booking/5/extend
    @GetMapping("/{id}/extend")
    public String extendForm(@PathVariable int id, Model model, RedirectAttributes redirect) {
        return openFilledForm(id, "extend", model, redirect);
    }

    // POST /booking/5/extend
    @PostMapping("/{id}/extend")
    public String extend(@PathVariable int id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                         Model model, RedirectAttributes redirect) {
        try {
            bookingService.extendStay(id, dateTo);
            redirect.addFlashAttribute("success", "Booking B-" + id + " extended to " + dateTo);
            return "redirect:/booking/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            // Show the saved booking again, with the date that was typed in
            Booking booking = bookingService.getBooking(id);
            prepareForm(model, "extend", id, booking.getGuestId(), booking.getRoomId(),
                    booking.getDateFrom(), dateTo);
            return "booking/form";
        }
    }

    // Loads a booking and shows the form filled with its values (edit and extend)
    private String openFilledForm(int id, String mode, Model model, RedirectAttributes redirect) {
        try {
            Booking b = bookingService.getBooking(id);
            prepareForm(model, mode, id, b.getGuestId(), b.getRoomId(), b.getDateFrom(), b.getDateTo());
            return "booking/form";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/list";
        }
    }

    // Everything form.jsp needs: mode, the values for the boxes and the dropdown choices
    private void prepareForm(Model model, String mode, int bookingId, Integer guestId, Integer roomId,
                             LocalDate dateFrom, LocalDate dateTo) {
        model.addAttribute("mode", mode);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("guestId", guestId);
        model.addAttribute("roomId", roomId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("guests", bookingService.getGuestOptions());
        model.addAttribute("rooms", bookingService.getRoomOptions());
    }

    // "12" -> 12, anything else ("new", empty, null) -> null
    private Integer parseIdOrNull(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // An empty dropdown arrives as null; treat it as 0 (invalid id)
    private int toInt(Integer value) {
        return value == null ? 0 : value;
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
