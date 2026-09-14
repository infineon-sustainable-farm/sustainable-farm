package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingOccupancyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/activities")
    public List<AgriActivityResponse> activities() {
        return bookingService.listActivities();
    }

    @PostMapping("/activities")
    public ResponseEntity<AgriActivityResponse> createActivity(
            @Valid @RequestBody AgriActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createActivity(request));
    }

    @PutMapping("/activities/{id}")
    public AgriActivityResponse updateActivity(
            @PathVariable Long id, @Valid @RequestBody AgriActivityRequest request) {
        return bookingService.updateActivity(id, request);
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> deactivateActivity(@PathVariable Long id) {
        bookingService.deactivateActivity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listBookings(status, activityId, date);
    }

    @GetMapping("/bookings/occupancy")
    public List<BookingOccupancyResponse> occupancy() {
        return bookingService.getOccupancy();
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @PutMapping("/bookings/{id}")
    public BookingResponse updateBooking(@PathVariable Long id,
                                         @Valid @RequestBody BookingRequest request) {
        return bookingService.updateBooking(id, request);
    }

    @PostMapping("/bookings/{id}/pay")
    public BookingResponse markPaid(@PathVariable Long id) {
        return bookingService.markPaid(id);
    }

    @PostMapping("/bookings/{id}/confirm")
    public BookingResponse confirmBooking(@PathVariable Long id) {
        return bookingService.confirmBooking(id);
    }

    @PostMapping("/bookings/{id}/complete")
    public BookingResponse completeBooking(@PathVariable Long id) {
        return bookingService.completeBooking(id);
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }
}