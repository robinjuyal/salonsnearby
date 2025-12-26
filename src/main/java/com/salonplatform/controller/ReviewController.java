package com.salonplatform.controller;

import com.salonplatform.dto.ApiResponse;
import com.salonplatform.dto.CreateReviewRequest;
import com.salonplatform.dto.ReviewResponse;
import com.salonplatform.security.CurrentUser;
import com.salonplatform.security.UserPrincipal;
import com.salonplatform.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review and rating management")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Create review
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create review", description = "Customer creates review for completed booking")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse response = reviewService.createReview(currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }

    /**
     * Get salon reviews
     */
    @GetMapping("/salon/{salonId}")
    @Operation(summary = "Get salon reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getSalonReviews(
            @PathVariable UUID salonId) {

        List<ReviewResponse> reviews = reviewService.getSalonReviews(salonId);

        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviews));
    }

    /**
     * Get barber reviews
     */
    @GetMapping("/barber/{barberId}")
    @Operation(summary = "Get barber reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getBarberReviews(
            @PathVariable UUID barberId) {

        List<ReviewResponse> reviews = reviewService.getBarberReviews(barberId);

        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviews));
    }

    /**
     * Delete review
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @CurrentUser UserPrincipal currentUser) {

        reviewService.deleteReview(reviewId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }
}
