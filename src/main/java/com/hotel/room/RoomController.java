package com.hotel.room;

import com.hotel.common.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Room pages. Everyone logged in can view rooms;
 * only ADMIN can add, edit and delete (checked in LoginInterceptor).
 */
@Controller
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Room is abstract, so Spring can't create one from the form by itself.
     * This runs before each request and creates the right subclass from the
     * "roomType" value, then Spring fills in the form fields on that object.
     */
    @ModelAttribute("room")
    public Room roomFromType(@RequestParam(value = "roomType", required = false) String roomType) {
        if ("SUITE".equalsIgnoreCase(roomType)) {
            return new SuiteRoom();
        }
        if ("DOUBLE".equalsIgnoreCase(roomType)) {
            return new DoubleRoom();
        }
        return new SingleRoom();
    }

    @GetMapping
    public String listRooms(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "status", required = false) RoomStatus status,
                            @RequestParam(value = "type", required = false) RoomType type,
                            Model model) {
        List<Room> rooms;
        if (status != null) {
            rooms = roomService.getByStatus(status);
        } else if (type != null) {
            rooms = roomService.getByType(type);
        } else {
            rooms = roomService.search(keyword);
        }
        model.addAttribute("roomList", rooms);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("roomTypes", RoomType.values());
        return "room/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@ModelAttribute("room") Room room, Model model) {
        addFormOptions(model, false);
        return "room/form";
    }

    @PostMapping("/create")
    public String createRoom(@ModelAttribute("room") Room room,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Room saved = roomService.create(room);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Room " + saved.getRoomNumber() + " added successfully!");
            return "redirect:/room";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            addFormOptions(model, false);
            return "room/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("room", roomService.getById(id));
        addFormOptions(model, true);
        return "room/form";
    }

    @PostMapping("/edit/{id}")
    public String updateRoom(@PathVariable("id") String id,
                             @ModelAttribute("room") Room room,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            roomService.update(id, room);
            redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully!");
            return "redirect:/room";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            room.setRoomId(id);
            addFormOptions(model, true);
            return "room/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteRoom(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            roomService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/room";
    }

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("logs", roomService.readLog());
        return "room/history";
    }

    private void addFormOptions(Model model, boolean isEdit) {
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("isEdit", isEdit);
    }
}
