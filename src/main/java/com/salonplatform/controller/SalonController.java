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
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
@Tag(name = "Salons", description = "Salon discovery and management")
class SalonController {

    private final SalonService salonService;

    /**
     * Find nearby salons
     */
    @GetMapping("/nearby")
    @Operation(summary = "Find nearby salons",
            description = "Discover salons near your location")
    public ResponseEntity<ApiResponse<List<SalonResponse>>> findNearbySalons(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {

        List<SalonResponse> salons = salonService.findNearbySalons(
                latitude, longitude, radiusKm);

        return ResponseEntity.ok(ApiResponse.success("Nearby salons", salons));
    }

    /**
     * Search salons
     */
    @GetMapping("/search")
    @Operation(summary = "Search salons")
    public ResponseEntity<ApiResponse<List<SalonResponse>>> searchSalons(
            @RequestParam String keyword) {

        List<SalonResponse> salons = salonService.searchSalons(keyword);

        return ResponseEntity.ok(ApiResponse.success("Search results", salons));
    }

    /**
     * Get salon details
     */
    @GetMapping("/{salonId}")
    @Operation(summary = "Get salon details")
    public ResponseEntity<ApiResponse<SalonDetailResponse>> getSalonDetails(
            @PathVariable UUID salonId) {

        SalonDetailResponse salon = salonService.getSalonDetails(salonId);

        return ResponseEntity.ok(ApiResponse.success("Salon details", salon));
    }

    /**
     * Get salon services
     */
    @GetMapping("/{salonId}/services")
    @Operation(summary = "Get salon services")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getSalonServices(
            @PathVariable UUID salonId) {

        List<ServiceResponse> services = salonService.getSalonServices(salonId);

        return ResponseEntity.ok(ApiResponse.success("Services", services));
    }

    /**
     * Get available time slots
     */
    @GetMapping("/{salonId}/available-slots")
    @Operation(summary = "Get available slots",
            description = "Check when salon has availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailableSlots(
            @PathVariable UUID salonId) {

        AvailabilityResponse availability = salonService.getAvailability(salonId);

        return ResponseEntity.ok(ApiResponse.success("Availability", availability));
    }

    /**
     * Register salon (Owner)
     */
    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Register new salon")
    public ResponseEntity<ApiResponse<SalonResponse>> registerSalon(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateSalonRequest request) {

        SalonResponse salon = salonService.createSalon(currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salon registered", salon));
    }

    /**
     * Update salon (Owner)
     */
    @PutMapping("/{salonId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update salon details")
    public ResponseEntity<ApiResponse<SalonResponse>> updateSalon(
            @PathVariable UUID salonId,
            @Valid @RequestBody UpdateSalonRequest request,
            @CurrentUser UserPrincipal currentUser) {

        SalonResponse salon = salonService.updateSalon(
                salonId, request, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Salon updated", salon));
    }

    /**
     * Toggle salon open/close
     */
    @PutMapping("/{salonId}/toggle-status")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Open/Close salon")
    public ResponseEntity<ApiResponse<Void>> toggleSalonStatus(
            @PathVariable UUID salonId,
            @RequestParam Boolean isOpen) {

        salonService.toggleSalonStatus(salonId, isOpen);

        return ResponseEntity.ok(ApiResponse.success("Salon status updated"));
    }
}
