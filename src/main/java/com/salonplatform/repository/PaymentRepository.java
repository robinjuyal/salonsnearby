package com.salonplatform.repository;

import com.salonplatform.entity.*;
import com.salonplatform.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    List<Payment> findByCustomerId(UUID customerId);
    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.booking.salon.id = :salonId " +
            "AND p.status = 'PAID' " +
            "AND p.createdAt BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> getTotalRevenue(@Param("salonId") UUID salonId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}