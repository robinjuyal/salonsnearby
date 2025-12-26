package com.salonplatform.service;

import com.salonplatform.entity.*;
import com.salonplatform.entity.Queue;
import com.salonplatform.enums.*;
import com.salonplatform.repository.*;
import com.salonplatform.dto.*;
import com.salonplatform.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SalonRepository salonRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BarberRepository barberRepository;
    private final QueueRepository queueRepository;
   // private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final QueueService queueService;

    @Value("${app.booking.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Value("${app.booking.auto-cancel-minutes}")
    private int autoCancelMinutes;

    /**
     * Create online booking - Customer books from app
     */
    @Transactional
    public BookingResponse createOnlineBooking(UUID customerId, CreateBookingRequest request) {
        log.info("Creating online booking for customer: {}", customerId);

        // Validate customer
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.isBlacklisted()) {
            throw new BusinessException("Customer is temporarily blocked due to multiple no-shows");
        }

        // Validate salon
        Salon salon = salonRepository.findById(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        if (!salon.getAcceptsOnlineBooking()) {
            throw new BusinessException("This salon does not accept online bookings");
        }

        // Validate service
        com.salonplatform.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (!service.getIsActive()) {
            throw new BusinessException("This service is currently unavailable");
        }

        // Check online booking capacity
        long onlineBookingsCount = bookingRepository.countOnlineBookingsToday(salon.getId());
        // Logic to limit online bookings based on salon settings

        // Calculate estimated start time based on current queue
        LocalDateTime estimatedStartTime = calculateNextAvailableSlot(salon.getId());

        // Assign barber (load balancing)
        Barber assignedBarber = assignBarber(salon.getId());

        // Create booking
        Booking booking = Booking.builder()
                .customer(customer)
                .salon(salon)
                .barber(assignedBarber)
                .service(service)
                .bookingType(BookingType.ONLINE)
                .status(BookingStatus.PENDING)
                .estimatedStartTime(estimatedStartTime)
                .estimatedDurationMinutes(service.getDurationMinutes())
                .amount(service.getPrice())
                .paymentStatus(PaymentStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                .build();

        booking = bookingRepository.save(booking);

//         Create payment order
//        PaymentOrderResponse paymentOrder = paymentService.createPaymentOrder(
//                booking.getId(),
//                booking.getAmount(),
//                customer.getPhone(),
//                customer.getEmail()
//        );
//
//        booking.setPaymentId(paymentOrder.getOrderId());
        bookingRepository.save(booking);

        log.info("Online booking created successfully: {}", booking.getId());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .salonName(salon.getName())
                .serviceName(service.getName())
                .estimatedStartTime(estimatedStartTime)
                .estimatedDurationMinutes(service.getDurationMinutes())
                .amount(service.getPrice())
//                .paymentOrderId(paymentOrder.getOrderId())
//                .paymentKey(paymentOrder.getKey())
                .status(booking.getStatus().toString())
                .build();
    }

    /**
     * Confirm booking after successful payment
     */
    @Transactional
    public void confirmBooking(UUID bookingId, String paymentId) {
        log.info("Confirming booking: {} with payment: {}", bookingId, paymentId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking cannot be confirmed in current status");
        }

        // Update booking status
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaymentId(paymentId);
        bookingRepository.save(booking);

        // Add to queue
        queueService.addToQueue(booking);

        // Update customer stats
        booking.getCustomer().incrementTotalBookings();
        userRepository.save(booking.getCustomer());

        // Send confirmation notification
        notificationService.sendBookingConfirmation(booking);

        log.info("Booking confirmed and added to queue: {}", bookingId);
    }

    /**
     * Create walk-in booking - Barber adds customer at salon
     */
    @Transactional
    public BookingResponse createWalkInBooking(UUID salonId, UUID barberId, CreateWalkInRequest request) {
        log.info("Creating walk-in booking for salon: {}", salonId);

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));

        com.salonplatform.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // For walk-in, create guest user or use existing customer
        User customer = request.getCustomerPhone() != null
                ? userRepository.findByPhone(request.getCustomerPhone())
                .orElse(createGuestUser(request.getCustomerPhone(), request.getCustomerName()))
                : createGuestUser(null, request.getCustomerName());

        LocalDateTime estimatedStartTime = calculateNextAvailableSlot(salonId);

        Booking booking = Booking.builder()
                .customer(customer)
                .salon(salon)
                .barber(barber)
                .service(service)
                .bookingType(BookingType.WALKIN)
                .status(BookingStatus.CONFIRMED) // Walk-ins are immediately confirmed
                .estimatedStartTime(estimatedStartTime)
                .estimatedDurationMinutes(service.getDurationMinutes())
                .amount(service.getPrice())
                .paymentStatus(PaymentStatus.PENDING) // Will pay after service
                .build();

        booking = bookingRepository.save(booking);

        // Add to queue immediately
        queueService.addToQueue(booking);

        log.info("Walk-in booking created: {}", booking.getId());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .salonName(salon.getName())
                .serviceName(service.getName())
                .estimatedStartTime(estimatedStartTime)
                .queuePosition(booking.getQueuePosition())
                .status(booking.getStatus().toString())
                .build();
    }

    /**
     * Start service - Barber begins working on customer
     */
    @Transactional
    public void startService(UUID bookingId) {
        log.info("Starting service for booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Booking must be confirmed to start service");
        }

        booking.markStarted();
        bookingRepository.save(booking);

        // Update queue status
        queueService.markInService(booking.getId());

        // Notify customer (if next in queue)
        notificationService.sendServiceStarted(booking);

        log.info("Service started for booking: {}", bookingId);
    }

    /**
     * Complete service - Barber finishes work
     */
    @Transactional
    public void completeService(UUID bookingId) {
        log.info("Completing service for booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BusinessException("Service must be in progress to complete");
        }

        booking.markCompleted();
        bookingRepository.save(booking);

        // Remove from queue
        queueService.removeFromQueue(booking.getId());

        // Update barber stats
        Barber barber = booking.getBarber();
        if (barber != null) {
            barber.setTotalServices(barber.getTotalServices() + 1);
            barberRepository.save(barber);
        }

        // Notify next customer in queue
        queueService.notifyNextInQueue(booking.getSalon().getId());

        // Request review
        notificationService.sendReviewRequest(booking);

        log.info("Service completed for booking: {}", bookingId);
    }

    /**
     * Cancel booking
     */
    @Transactional
    public void cancelBooking(UUID bookingId, UUID userId, String reason) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.COMPLETED ||
                booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot cancel booking in current status");
        }

        User cancelledBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(cancelledBy);
        bookingRepository.save(booking);

        // Remove from queue if present
        queueService.removeFromQueue(bookingId);

        // Process refund if payment was made
//        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
//            BigDecimal refundAmount = calculateRefundAmount(booking);
//            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
//                paymentService.processRefund(booking.getId(), refundAmount, reason);
//            }
//        }

        // Send cancellation notification
        notificationService.sendBookingCancellation(booking);

        log.info("Booking cancelled: {}", bookingId);
    }

    /**
     * Mark booking as no-show
     */
    @Transactional
    public void markNoShow(UUID bookingId) {
        log.info("Marking booking as no-show: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);

        // Update customer no-show count
        User customer = booking.getCustomer();
        customer.incrementNoShowCount();
        userRepository.save(customer);

        // Remove from queue
        queueService.removeFromQueue(bookingId);

        // No refund for no-shows

        log.info("Booking marked as no-show: {}", bookingId);
    }

    /**
     * Auto-cancel overdue bookings
     */
    @Transactional
    public void processOverdueBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(autoCancelMinutes);
        List<Booking> overdueBookings = bookingRepository.findOverdueBookings(cutoffTime);

        log.info("Processing {} overdue bookings", overdueBookings.size());

        for (Booking booking : overdueBookings) {
            if (booking.getBookingType() == BookingType.ONLINE) {
                markNoShow(booking.getId());
            }
        }
    }

    // Helper methods

    private LocalDateTime calculateNextAvailableSlot(UUID salonId) {
        List<Queue> currentQueue = queueRepository.findBySalonIdOrderByPositionAsc(salonId);

        if (currentQueue.isEmpty()) {
            return LocalDateTime.now();
        }

        // Calculate based on queue
        int totalWaitMinutes = currentQueue.stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING)
                .mapToInt(q -> q.getBooking().getEstimatedDurationMinutes())
                .sum();

        return LocalDateTime.now().plusMinutes(totalWaitMinutes);
    }

    private Barber assignBarber(UUID salonId) {
        List<Barber> availableBarbers = barberRepository
                .findAvailableBarbersOrderByLoad(salonId);

        if (availableBarbers.isEmpty()) {
            return null; // No specific barber assigned
        }

        return availableBarbers.get(0); // Assign least busy barber
    }

    private User createGuestUser(String phone, String name) {
        return userRepository.save(User.builder()
                .phone(phone != null ? phone : "GUEST_" + UUID.randomUUID())
                .fullName(name)
                .password("N/A")
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .build());
    }

    private BigDecimal calculateRefundAmount(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilBooking = java.time.Duration.between(now,
                booking.getEstimatedStartTime()).toHours();

        // Refund policy
        if (hoursUntilBooking > 24) {
            return booking.getAmount(); // 100% refund
        } else if (hoursUntilBooking > 2) {
            return booking.getAmount().multiply(BigDecimal.valueOf(0.5)); // 50% refund
        } else {
            return BigDecimal.ZERO; // No refund
        }
    }

    public List<BookingResponse> getCustomerBookings(UUID customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getSalonBookings(UUID salonId) {
        return bookingRepository.findActiveBookingsBySalon(salonId).stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .customerName(booking.getCustomer().getFullName())
                .salonName(booking.getSalon().getName())
                .serviceName(booking.getService().getName())
                .barberName(booking.getBarber() != null ? booking.getBarber().getName() : null)
                .estimatedStartTime(booking.getEstimatedStartTime())
                .actualStartTime(booking.getActualStartTime())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .queuePosition(booking.getQueuePosition())
                .amount(booking.getAmount())
                .status(booking.getStatus().toString())
                .paymentStatus(booking.getPaymentStatus().toString())
                .bookingType(booking.getBookingType().toString())
                .build();
    }
}