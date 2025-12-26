package com.salonplatform.entity;

import com.salonplatform.enums.QueueStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Queue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id")
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    @Enumerated(EnumType.STRING)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}