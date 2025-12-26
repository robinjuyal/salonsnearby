package com.salonplatform.repository;

import com.salonplatform.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findBySalonIdOrderByCreatedAtDesc(UUID salonId);
    List<Review> findByBarberId(UUID barberId);
    Optional<Review> findByBookingId(UUID bookingId);
    boolean existsByBookingId(UUID bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.salon.id = :salonId")
    Optional<Double> findAverageRatingBySalon(@Param("salonId") UUID salonId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.barber.id = :barberId")
    Optional<Double> findAverageRatingByBarber(@Param("barberId") UUID barberId);
}
