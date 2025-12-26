package com.salonplatform.service;

import com.salonplatform.entity.*;
import com.salonplatform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendBookingConfirmation(Booking booking) {
        String message = String.format(
                "Your booking at %s has been confirmed! Service: %s, Queue position: #%d",
                booking.getSalon().getName(),
                booking.getService().getName(),
                booking.getQueuePosition()
        );
        createNotification(booking.getCustomer(), booking, "BOOKING_CONFIRMED", "Booking Confirmed", message, "IN_APP");
    }

    @Transactional
    public void sendServiceStarted(Booking booking) {
        String message = String.format("Your service at %s has started!", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "SERVICE_STARTED", "Service Started", message, "IN_APP");
    }

    @Transactional
    public void sendYourTurnNext(Booking booking) {
        String message = String.format("You're next at %s! Please be ready.", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "TURN_NEXT", "You're Next!", message, "IN_APP");
    }

    @Transactional
    public void sendGetReady(Booking booking) {
        String message = String.format("Get ready! You're 2nd in queue at %s.", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "GET_READY", "Get Ready", message, "IN_APP");
    }

    @Transactional
    public void sendQueuePositionUpdate(Booking booking, int newPosition) {
        String message = String.format("Your position at %s moved to #%d", booking.getSalon().getName(), newPosition);
        createNotification(booking.getCustomer(), booking, "QUEUE_UPDATE", "Queue Updated", message, "IN_APP");
    }

    @Transactional
    public void sendMovedToEndOfQueue(Booking booking) {
        String message = String.format("You arrived late. Your position moved to end of queue at %s", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "LATE_ARRIVAL", "Queue Position Changed", message, "IN_APP");
    }

    @Transactional
    public void sendBookingCancellation(Booking booking) {
        String message = String.format("Your booking at %s has been cancelled.", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "BOOKING_CANCELLED", "Booking Cancelled", message, "IN_APP");
    }

    @Transactional
    public void sendReviewRequest(Booking booking) {
        String message = String.format("How was your experience at %s? Please rate your service.", booking.getSalon().getName());
        createNotification(booking.getCustomer(), booking, "REVIEW_REQUEST", "Rate Your Experience", message, "IN_APP");
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId, int limit) {
        return notificationRepository.findRecentNotifications(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    private void createNotification(User user, Booking booking, String type, String title, String message, String sentVia) {
        Notification notification = Notification.builder()
                .user(user)
                .booking(booking)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .sentVia(sentVia)
                .build();
        notificationRepository.save(notification);
    }
}