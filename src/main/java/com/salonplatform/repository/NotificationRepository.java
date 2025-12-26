package com.salonplatform.repository;

import com.salonplatform.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    long countByUserIdAndIsReadFalse(UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") UUID userId,
                                               org.springframework.data.domain.Pageable pageable);
}