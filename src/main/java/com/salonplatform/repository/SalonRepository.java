package com.salonplatform.repository;

import com.salonplatform.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.*;

@Repository
public interface SalonRepository extends JpaRepository<Salon, UUID> {
    List<Salon> findByOwnerId(UUID ownerId);
    List<Salon> findByCity(String city);
    List<Salon> findByIsOpenTrue();

    @Query("SELECT s FROM Salon s WHERE s.isOpen = true AND s.acceptsOnlineBooking = true " +
            "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(s.latitude)))) <= :radiusKm ORDER BY " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(s.latitude))))")
    List<Salon> findNearbySalons(@Param("latitude") BigDecimal latitude,
                                 @Param("longitude") BigDecimal longitude,
                                 @Param("radiusKm") double radiusKm);

    @Query("SELECT s FROM Salon s WHERE s.isOpen = true " +
            "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.city) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Salon> searchSalons(@Param("keyword") String keyword);
}
