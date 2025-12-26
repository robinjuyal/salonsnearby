package com.salonplatform.repository;

import com.salonplatform.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface SalonImageRepository extends JpaRepository<SalonImage, UUID> {
    List<SalonImage> findBySalonId(UUID salonId);
    Optional<SalonImage> findBySalonIdAndIsPrimaryTrue(UUID salonId);
}
