package com.salonplatform.repository;

import com.salonplatform.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.*;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {
    List<Barber> findBySalonId(UUID salonId);
    List<Barber> findBySalonIdAndIsAvailableTrue(UUID salonId);
    Optional<Barber> findByUserId(UUID userId);

    @Query("SELECT b FROM Barber b WHERE b.salon.id = :salonId " +
            "AND b.isAvailable = true " +
            "ORDER BY b.totalServices ASC")
    List<Barber> findAvailableBarbersOrderByLoad(@Param("salonId") UUID salonId);
}
