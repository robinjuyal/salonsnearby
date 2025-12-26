package com.salonplatform.entity;

import com.salonplatform.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "salons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Salon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String phone;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime = LocalTime.of(9, 0);

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime = LocalTime.of(21, 0);

    @Column(name = "is_open")
    private Boolean isOpen = true;

    @Column(name = "accepts_online_booking")
    private Boolean acceptsOnlineBooking = true;

    @Column(name = "online_booking_percentage")
    private Integer onlineBookingPercentage = 70;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier")
    private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalonImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Barber> barbers = new ArrayList<>();

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Service> services = new ArrayList<>();

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    public boolean isOpenNow() {
        LocalTime now = LocalTime.now();
        return isOpen && now.isAfter(openingTime) && now.isBefore(closingTime);
    }

    public void updateRating(BigDecimal newRating) {
        if (totalReviews == 0) {
            this.rating = newRating;
        } else {
            BigDecimal totalRating = this.rating.multiply(BigDecimal.valueOf(totalReviews));
            totalRating = totalRating.add(newRating);
            this.rating = totalRating.divide(BigDecimal.valueOf(totalReviews + 1), 2, BigDecimal.ROUND_HALF_UP);
        }
        this.totalReviews++;
    }
}
