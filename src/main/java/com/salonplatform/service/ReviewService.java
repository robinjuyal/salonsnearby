package com.salonplatform.service;

import com.salonplatform.dto.CreateReviewRequest;
import com.salonplatform.dto.ReviewResponse;
import com.salonplatform.entity.*;
import com.salonplatform.enums.BookingStatus;
import com.salonplatform.exception.BusinessException;
import com.salonplatform.exception.ResourceNotFoundException;
import com.salonplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final SalonRepository salonRepository;
    private final BarberRepository barberRepository;

    @Transactional
    public ReviewResponse createReview(UUID customerId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("You can only review completed bookings");
        }

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BusinessException("You have already reviewed this booking");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }

        Review review = Review.builder()
                .booking(booking)
                .customer(booking.getCustomer())
                .salon(booking.getSalon())
                .barber(booking.getBarber())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        updateSalonRating(booking.getSalon().getId());

        if (booking.getBarber() != null) {
            updateBarberRating(booking.getBarber().getId());
        }

        return toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getSalonReviews(UUID salonId) {
        return reviewRepository.findBySalonIdOrderByCreatedAtDesc(salonId)
                .stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getBarberReviews(UUID barberId) {
        return reviewRepository.findByBarberId(barberId)
                .stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getCustomer().getId().equals(userId)) {
            throw new BusinessException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        updateSalonRating(review.getSalon().getId());
        if (review.getBarber() != null) {
            updateBarberRating(review.getBarber().getId());
        }
    }

    private void updateSalonRating(UUID salonId) {
        Double avgRating = reviewRepository.findAverageRatingBySalon(salonId).orElse(0.0);
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));
        salon.setRating(BigDecimal.valueOf(avgRating));
        salon.setTotalReviews(reviewRepository.findBySalonIdOrderByCreatedAtDesc(salonId).size());
        salonRepository.save(salon);
    }

    private void updateBarberRating(UUID barberId) {
        Double avgRating = reviewRepository.findAverageRatingByBarber(barberId).orElse(0.0);
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));
        barber.setRating(BigDecimal.valueOf(avgRating));
        barberRepository.save(barber);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .customerName(review.getCustomer().getFullName())
                .salonName(review.getSalon().getName())
                .barberName(review.getBarber() != null ? review.getBarber().getName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}