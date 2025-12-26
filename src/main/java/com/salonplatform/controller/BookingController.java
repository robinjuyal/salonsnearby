package com.salonplatform.controller;

import com.salonplatform.dto.*;
import com.salonplatform.service.*;
import com.salonplatform.security.CurrentUser;
import com.salonplatform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;
    private final QueueService queueService;

    /**
     * Create online booking (Customer)
     */
    @PostMapping("/online")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create online booking",
            description = "Customer creates a booking and gets payment order")
    public ResponseEntity<ApiResponse<BookingResponse>> createOnlineBooking(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse response = bookingService.createOnlineBooking(
                currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    /**
     * Confirm booking after payment (Webhook/Frontend)
     */
    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm booking after payment")
    public ResponseEntity<ApiResponse<Void>> confirmBooking(
            @PathVariable UUID bookingId,
            @RequestParam String paymentId) {

        bookingService.confirmBooking(bookingId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Booking confirmed"));
    }

    /**
     * Create walk-in booking (Barber/Salon)
     */
    @PostMapping("/walkin")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Add walk-in customer",
            description = "Barber adds a walk-in customer to the queue")
    public ResponseEntity<ApiResponse<BookingResponse>> createWalkInBooking(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateWalkInRequest request) {

        BookingResponse response = bookingService.createWalkInBooking(
                request.getSalonId(),
                currentUser.getId(),
                request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Walk-in added to queue", response));
    }

    /**
     * Start service (Barber)
     */
    @PutMapping("/{bookingId}/start")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Start service", description = "Barber starts working on customer")
    public ResponseEntity<ApiResponse<Void>> startService(
            @PathVariable UUID bookingId) {

        bookingService.startService(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Service started"));
    }

    /**
     * Complete service (Barber)
     */
    @PutMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Complete service", description = "Barber finishes work")
    public ResponseEntity<ApiResponse<Void>> completeService(
            @PathVariable UUID bookingId) {

        bookingService.completeService(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Service completed"));
    }

    /**
     * Cancel booking
     */
    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SALON_OWNER', 'BARBER')")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable UUID bookingId,
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) String reason) {

        bookingService.cancelBooking(bookingId, currentUser.getId(), reason);

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled"));
    }

    /**
     * Mark as no-show (Barber)
     */
    @PutMapping("/{bookingId}/no-show")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Mark as no-show")
    public ResponseEntity<ApiResponse<Void>> markNoShow(
            @PathVariable UUID bookingId) {

        bookingService.markNoShow(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Marked as no-show"));
    }

    /**
     * Get customer's bookings
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my bookings", description = "Customer views their bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @CurrentUser UserPrincipal currentUser) {

        List<BookingResponse> bookings = bookingService.getCustomerBookings(
                currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

    /**
     * Get salon bookings (Salon dashboard)
     */
    @GetMapping("/salon/{salonId}")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Get salon bookings", description = "View all salon bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getSalonBookings(
            @PathVariable UUID salonId) {

        List<BookingResponse> bookings = bookingService.getSalonBookings(salonId);

        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

}
