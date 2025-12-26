package com.salonplatform.repository;

import com.salonplatform.entity.Queue;
import com.salonplatform.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface QueueRepository extends JpaRepository<Queue, UUID> {
    List<Queue> findBySalonIdAndStatusOrderByPositionAsc(UUID salonId, QueueStatus status);
    List<Queue> findBySalonIdOrderByPositionAsc(UUID salonId);
    Optional<Queue> findBySalonIdAndPosition(UUID salonId, Integer position);
    Optional<Queue> findByBookingId(UUID bookingId);

    @Query("SELECT MAX(q.position) FROM Queue q WHERE q.salon.id = :salonId")
    Optional<Integer> findMaxPositionBySalon(@Param("salonId") UUID salonId);

    @Query("SELECT COUNT(q) FROM Queue q WHERE q.salon.id = :salonId " +
            "AND q.status = 'WAITING'")
    long countWaitingCustomers(@Param("salonId") UUID salonId);
}
