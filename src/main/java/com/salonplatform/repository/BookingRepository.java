package com.salonplatform.repository;
import com.salonplatform.entity.*;
import com.salonplatform.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;


@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByCustomerId(UUID customerId);
    List<Booking> findBySalonId(UUID salonId);
    List<Booking> findByBarberId(UUID barberId);
    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.salon.id = :salonId " +
            "AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS') " +
            "ORDER BY b.estimatedStartTime ASC")
    List<Booking> findActiveBookingsBySalon(@Param("salonId") UUID salonId);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "ORDER BY b.estimatedStartTime DESC")
    List<Booking> findUpcomingBookingsByCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT b FROM Booking b WHERE b.salon.id = :salonId " +
            "AND b.estimatedStartTime BETWEEN :startDate AND :endDate " +
            "ORDER BY b.estimatedStartTime ASC")
    List<Booking> findBySalonAndDateRange(@Param("salonId") UUID salonId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.salon.id = :salonId " +
            "AND b.bookingType = 'ONLINE' " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND b.estimatedStartTime >= CURRENT_TIMESTAMP")
    long countOnlineBookingsToday(@Param("salonId") UUID salonId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND b.estimatedStartTime < :cutoffTime " +
            "AND b.actualStartTime IS NULL")
    List<Booking> findOverdueBookings(@Param("cutoffTime") LocalDateTime cutoffTime);
}
