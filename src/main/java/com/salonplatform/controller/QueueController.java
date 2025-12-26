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
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "Queue management APIs")
class QueueController {

    private final QueueService queueService;

    /**
     * Get salon queue (Real-time)
     */
    @GetMapping("/salon/{salonId}")
    @Operation(summary = "Get salon queue", description = "View current queue for salon")
    public ResponseEntity<ApiResponse<List<QueueResponse>>> getSalonQueue(
            @PathVariable UUID salonId) {

        List<QueueResponse> queue = queueService.getSalonQueue(salonId);

        return ResponseEntity.ok(ApiResponse.success("Queue retrieved", queue));
    }

    /**
     * Get customer's queue position
     */
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get queue status", description = "Check position in queue")
    public ResponseEntity<ApiResponse<QueueResponse>> getQueueStatus(
            @PathVariable UUID bookingId) {

        QueueResponse status = queueService.getCustomerQueueStatus(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Queue status", status));
    }

    /**
     * Get queue statistics (Dashboard)
     */
    @GetMapping("/salon/{salonId}/stats")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Get queue statistics")
    public ResponseEntity<ApiResponse<QueueStatsResponse>> getQueueStats(
            @PathVariable UUID salonId) {

        QueueStatsResponse stats = queueService.getQueueStats(salonId);

        return ResponseEntity.ok(ApiResponse.success("Queue stats", stats));
    }

    /**
     * Handle late arrival - move to end
     */
    @PutMapping("/booking/{bookingId}/late-arrival")
    @PreAuthorize("hasAnyRole('BARBER', 'SALON_OWNER')")
    @Operation(summary = "Handle late arrival",
            description = "Move late customer to end of queue")
    public ResponseEntity<ApiResponse<Void>> handleLateArrival(
            @PathVariable UUID bookingId) {

        queueService.handleLateArrival(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Customer moved to end of queue"));
    }
}
