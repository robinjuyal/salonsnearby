package com.salonplatform.repository;

import com.salonplatform.entity.*;
import com.salonplatform.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findBySalonId(UUID salonId);
    List<Service> findBySalonIdAndIsActiveTrue(UUID salonId);
    List<Service> findByCategory(ServiceCategory category);
}
