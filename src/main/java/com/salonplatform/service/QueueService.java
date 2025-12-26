package com.salonplatform.service;

import com.salonplatform.entity.*;
import com.salonplatform.entity.Queue;
import com.salonplatform.enums.PaymentStatus;
import com.salonplatform.enums.QueueStatus;
import com.salonplatform.repository.*;
import com.salonplatform.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueRepository queueRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket updates

    /**
     * Add booking to queue
     */
    @Transactional
    public Queue addToQueue(Booking booking) {
        log.info("Adding booking {} to queue for salon {}",
                booking.getId(), booking.getSalon().getId());

        // Get current max position
        Integer maxPosition = queueRepository
                .findMaxPositionBySalon(booking.getSalon().getId())
                .orElse(0);

        int newPosition = maxPosition + 1;

        // Calculate estimated wait time
        int estimatedWait = calculateEstimatedWait(booking.getSalon().getId(), newPosition);

        Queue queueEntry = Queue.builder()
                .salon(booking.getSalon())
                .barber(booking.getBarber())
                .booking(booking)
                .customerName(booking.getCustomer().getFullName())
                .serviceName(booking.getService().getName())
                .position(newPosition)
                .estimatedWaitMinutes(estimatedWait)
                .status(QueueStatus.WAITING)
                .addedAt(LocalDateTime.now())
                .build();

        queueEntry = queueRepository.save(queueEntry);

        // Update booking with queue position
        booking.setQueuePosition(newPosition);
        bookingRepository.save(booking);

        // Broadcast queue update via WebSocket
        broadcastQueueUpdate(booking.getSalon().getId());

        log.info("Booking added to queue at position: {}", newPosition);

        return queueEntry;
    }

    /**
     * Move customer up when someone ahead completes/cancels
     */
    @Transactional
    public void reorderQueue(UUID salonId) {
        log.info("Reordering queue for salon: {}", salonId);

        List<Queue> queueList = queueRepository
                .findBySalonIdAndStatusOrderByPositionAsc(salonId, QueueStatus.WAITING);

        int position = 1;
        for (Queue queueEntry : queueList) {
            if (queueEntry.getPosition() != position) {
                queueEntry.setPosition(position);
                queueEntry.setEstimatedWaitMinutes(
                        calculateEstimatedWait(salonId, position));

                // Update booking
                Booking booking = queueEntry.getBooking();
                booking.setQueuePosition(position);

                LocalDateTime newEstimatedTime = LocalDateTime.now()
                        .plusMinutes(queueEntry.getEstimatedWaitMinutes());
                booking.setEstimatedStartTime(newEstimatedTime);

                queueRepository.save(queueEntry);
                bookingRepository.save(booking);
            }
            position++;
        }

        // Broadcast queue update
        broadcastQueueUpdate(salonId);

        // Notify customers about position changes
        notifyQueueChanges(salonId);
    }

    /**
     * Mark booking as in-service
     */
    @Transactional
    public void markInService(UUID bookingId) {
        Queue queueEntry = queueRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        queueEntry.setStatus(QueueStatus.IN_SERVICE);
        queueRepository.save(queueEntry);

        broadcastQueueUpdate(queueEntry.getSalon().getId());
    }

    /**
     * Remove from queue (completed/cancelled/no-show)
     */
    @Transactional
    public void removeFromQueue(UUID bookingId) {
        log.info("Removing booking {} from queue", bookingId);

        Optional<Queue> queueEntryOpt = queueRepository.findByBookingId(bookingId);

        if (queueEntryOpt.isPresent()) {
            Queue queueEntry = queueEntryOpt.get();
            UUID salonId = queueEntry.getSalon().getId();

            queueEntry.setStatus(QueueStatus.COMPLETED);
            queueRepository.save(queueEntry);

            // Reorder remaining queue
            reorderQueue(salonId);

            log.info("Booking removed from queue and queue reordered");
        }
    }

    /**
     * Get current queue for salon
     */
    public List<QueueResponse> getSalonQueue(UUID salonId) {
        List<Queue> queue = queueRepository.findBySalonIdOrderByPositionAsc(salonId);

        return queue.stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING ||
                        q.getStatus() == QueueStatus.IN_SERVICE)
                .map(this::toQueueResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get customer's position in queue
     */
    public QueueResponse getCustomerQueueStatus(UUID bookingId) {
        Queue queueEntry = queueRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Not in queue"));

        return toQueueResponse(queueEntry);
    }

    /**
     * Notify next customer that their turn is coming
     */
    @Transactional
    public void notifyNextInQueue(UUID salonId) {
        List<Queue> waitingQueue = queueRepository
                .findBySalonIdAndStatusOrderByPositionAsc(salonId, QueueStatus.WAITING);

        if (!waitingQueue.isEmpty()) {
            Queue nextCustomer = waitingQueue.get(0);

            // Notify if next in line (position 1)
            if (nextCustomer.getPosition() == 1) {
                notificationService.sendYourTurnNext(nextCustomer.getBooking());
            }

            // Notify if 2nd in line
            if (waitingQueue.size() > 1) {
                Queue secondCustomer = waitingQueue.get(1);
                notificationService.sendGetReady(secondCustomer.getBooking());
            }
        }
    }

    /**
     * Handle late arrivals - move to end of queue
     */
    @Transactional
    public void handleLateArrival(UUID bookingId) {
        log.info("Handling late arrival for booking: {}", bookingId);

        Queue queueEntry = queueRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Not in queue"));

        UUID salonId = queueEntry.getSalon().getId();

        // Mark as skipped temporarily
        queueEntry.setStatus(QueueStatus.SKIPPED);
        queueRepository.save(queueEntry);

        // Reorder queue (others move up)
        reorderQueue(salonId);

        // Add back to end of queue
        Integer maxPosition = queueRepository
                .findMaxPositionBySalon(salonId)
                .orElse(0);

        queueEntry.setPosition(maxPosition + 1);
        queueEntry.setStatus(QueueStatus.WAITING);
        queueEntry.setEstimatedWaitMinutes(
                calculateEstimatedWait(salonId, maxPosition + 1));
        queueRepository.save(queueEntry);

        // Update booking
        Booking booking = queueEntry.getBooking();
        booking.setQueuePosition(maxPosition + 1);
        booking.setEstimatedStartTime(LocalDateTime.now()
                .plusMinutes(queueEntry.getEstimatedWaitMinutes()));
        bookingRepository.save(booking);

        // Notify customer
        notificationService.sendMovedToEndOfQueue(booking);

        broadcastQueueUpdate(salonId);

        log.info("Late arrival moved to position: {}", maxPosition + 1);
    }

    /**
     * Get queue statistics for salon dashboard
     */
    public QueueStatsResponse getQueueStats(UUID salonId) {
        List<Queue> allQueue = queueRepository.findBySalonIdOrderByPositionAsc(salonId);

        long waiting = allQueue.stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING)
                .count();

        long inService = allQueue.stream()
                .filter(q -> q.getStatus() == QueueStatus.IN_SERVICE)
                .count();

        int totalWaitMinutes = allQueue.stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING)
                .mapToInt(Queue::getEstimatedWaitMinutes)
                .sum();

        return QueueStatsResponse.builder()
                .totalInQueue(waiting)
                .currentlyServing(inService)
                .estimatedTotalWaitTime(totalWaitMinutes)
                .averageWaitTime(waiting > 0 ? totalWaitMinutes / waiting : 0)
                .build();
    }

    // Helper methods

    private int calculateEstimatedWait(UUID salonId, int position) {
        if (position <= 1) {
            return 0;
        }

        List<Queue> ahead = queueRepository.findBySalonIdOrderByPositionAsc(salonId)
                .stream()
                .filter(q -> q.getPosition() < position &&
                        (q.getStatus() == QueueStatus.WAITING ||
                                q.getStatus() == QueueStatus.IN_SERVICE))
                .collect(Collectors.toList());

        int totalMinutes = 0;
        for (Queue q : ahead) {
            if (q.getStatus() == QueueStatus.IN_SERVICE) {
                // Estimate remaining time for current service
                Booking booking = q.getBooking();
                if (booking.getActualStartTime() != null) {
                    long elapsed = java.time.Duration
                            .between(booking.getActualStartTime(), LocalDateTime.now())
                            .toMinutes();
                    int remaining = Math.max(0,
                            booking.getEstimatedDurationMinutes() - (int)elapsed);
                    totalMinutes += remaining;
                }
            } else {
                totalMinutes += q.getBooking().getEstimatedDurationMinutes();
            }
        }

        return totalMinutes;
    }

    private void broadcastQueueUpdate(UUID salonId) {
        List<QueueResponse> queueData = getSalonQueue(salonId);

        // Send via WebSocket to all subscribers of this salon
        messagingTemplate.convertAndSend(
                "/topic/salon/" + salonId + "/queue",
                queueData
        );

        log.debug("Broadcasted queue update for salon: {}", salonId);
    }

    private void notifyQueueChanges(UUID salonId) {
        List<Queue> queue = queueRepository
                .findBySalonIdAndStatusOrderByPositionAsc(salonId, QueueStatus.WAITING);

        for (Queue queueEntry : queue) {
            // Notify if moved up significantly (3+ positions)
            Booking booking = queueEntry.getBooking();
            if (booking.getQueuePosition() != null &&
                    booking.getQueuePosition() - queueEntry.getPosition() >= 3) {
                notificationService.sendQueuePositionUpdate(booking, queueEntry.getPosition());
            }
        }
    }

    private QueueResponse toQueueResponse(Queue queue) {
        Booking booking = queue.getBooking();

        return QueueResponse.builder()
                .queueId(queue.getId())
                .bookingId(booking.getId())
                .customerName(queue.getCustomerName())
                .serviceName(queue.getServiceName())
                .barberName(queue.getBarber() != null ? queue.getBarber().getName() : "Any")
                .position(queue.getPosition())
                .estimatedWaitMinutes(queue.getEstimatedWaitMinutes())
                .estimatedStartTime(booking.getEstimatedStartTime())
                .status(queue.getStatus().toString())
                .bookingType(booking.getBookingType().toString())
                .isPaid(booking.getPaymentStatus() == PaymentStatus.PAID)
                .addedAt(queue.getAddedAt())
                .build();
    }
}