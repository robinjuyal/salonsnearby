package com.salonplatform.service;

import com.salonplatform.dto.*;
import com.salonplatform.entity.*;
import com.salonplatform.enums.QueueStatus;
import com.salonplatform.enums.SubscriptionTier;
import com.salonplatform.enums.UserRole;
import com.salonplatform.exception.BusinessException;
import com.salonplatform.exception.ResourceNotFoundException;
import com.salonplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalonService {

    private final SalonRepository salonRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final BookingRepository bookingRepository;
    private final QueueRepository queueRepository;
    private final SalonImageRepository salonImageRepository;

    @Transactional
    public SalonResponse createSalon(UUID ownerId, CreateSalonRequest request) {
        log.info("Creating salon for owner: {}", ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        if (owner.getRole() != UserRole.SALON_OWNER && owner.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Only salon owners can create salons");
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());

        Salon salon = Salon.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .openingTime(request.getOpeningTime() != null ? request.getOpeningTime() : LocalTime.of(9, 0))
                .closingTime(request.getClosingTime() != null ? request.getClosingTime() : LocalTime.of(21, 0))
                .isOpen(true)
                .acceptsOnlineBooking(true)
                .onlineBookingPercentage(70)
                .rating(BigDecimal.ZERO)
                .totalReviews(0)
                .subscriptionTier(SubscriptionTier.FREE)
                .subscriptionExpiresAt(LocalDateTime.now().plusDays(30))
                .build();

        salon = salonRepository.save(salon);

        if (request.getServices() != null && !request.getServices().isEmpty()) {
            for (var serviceReq : request.getServices()) {
                com.salonplatform.entity.Service service = com.salonplatform.entity.Service.builder()
                        .salon(salon)
                        .name(serviceReq.getName())
                        .description(serviceReq.getDescription())
                        .price(serviceReq.getPrice())
                        .durationMinutes(serviceReq.getDurationMinutes())
                        .category(serviceReq.getCategory())
                        .isActive(true)
                        .build();
                serviceRepository.save(service);
            }
        }

        log.info("Salon created successfully: {}", salon.getId());
        return toSalonResponse(salon);
    }

    @Transactional
    public SalonResponse updateSalon(UUID salonId, UpdateSalonRequest request, UUID ownerId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        if (!salon.getOwner().getId().equals(ownerId)) {
            throw new BusinessException("You don't have permission to update this salon");
        }

        if (request.getName() != null) salon.setName(request.getName());
        if (request.getDescription() != null) salon.setDescription(request.getDescription());
        if (request.getAddress() != null) salon.setAddress(request.getAddress());
        if (request.getPhone() != null) salon.setPhone(request.getPhone());
        if (request.getOpeningTime() != null) salon.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null) salon.setClosingTime(request.getClosingTime());
        if (request.getOnlineBookingPercentage() != null) salon.setOnlineBookingPercentage(request.getOnlineBookingPercentage());

        salon = salonRepository.save(salon);
        return toSalonResponse(salon);
    }

    @Transactional(readOnly = true)
    public List<SalonResponse> findNearbySalons(Double latitude, Double longitude, Double radiusKm) {
        validateCoordinates(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));

        List<Salon> salons = salonRepository.findNearbySalons(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                radiusKm
        );

        return salons.stream()
                .map(this::toSalonResponseWithAvailability)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalonResponse> searchSalons(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return salonRepository.findByIsOpenTrue().stream()
                    .map(this::toSalonResponse)
                    .collect(Collectors.toList());
        }

        return salonRepository.searchSalons(keyword).stream()
                .map(this::toSalonResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalonDetailResponse getSalonDetails(UUID salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        List<ServiceResponse> services = serviceRepository.findBySalonIdAndIsActiveTrue(salonId)
                .stream()
                .map(this::toServiceResponse)
                .collect(Collectors.toList());

        List<BarberResponse> barbers = barberRepository.findBySalonIdAndIsAvailableTrue(salonId)
                .stream()
                .map(this::toBarberResponse)
                .collect(Collectors.toList());

        List<String> images = salonImageRepository.findBySalonId(salonId)
                .stream()
                .map(SalonImage::getImageUrl)
                .collect(Collectors.toList());

        long currentQueue = queueRepository.countWaitingCustomers(salonId);
        LocalDateTime nextAvailable = calculateNextAvailableTime(salonId);

        return SalonDetailResponse.builder()
                .id(salon.getId())
                .name(salon.getName())
                .description(salon.getDescription())
                .address(salon.getAddress())
                .city(salon.getCity())
                .state(salon.getState())
                .pincode(salon.getPincode())
                .latitude(salon.getLatitude())
                .longitude(salon.getLongitude())
                .phone(salon.getPhone())
                .openingTime(salon.getOpeningTime())
                .closingTime(salon.getClosingTime())
                .isOpen(salon.isOpenNow())
                .acceptsOnlineBooking(salon.getAcceptsOnlineBooking())
                .rating(salon.getRating())
                .totalReviews(salon.getTotalReviews())
                .services(services)
                .barbers(barbers)
                .images(images)
                .currentQueue(currentQueue)
                .nextAvailableTime(nextAvailable)
                .subscriptionTier(salon.getSubscriptionTier().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getSalonServices(UUID salonId) {
        return serviceRepository.findBySalonIdAndIsActiveTrue(salonId)
                .stream()
                .map(this::toServiceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        long currentQueue = queueRepository.countWaitingCustomers(salonId);
        LocalDateTime nextAvailable = calculateNextAvailableTime(salonId);

        int estimatedWaitMinutes = 0;
        if (nextAvailable != null) {
            estimatedWaitMinutes = (int) java.time.Duration
                    .between(LocalDateTime.now(), nextAvailable)
                    .toMinutes();
        }

        return AvailabilityResponse.builder()
                .salonId(salonId)
                .isOpen(salon.isOpenNow())
                .acceptsOnlineBooking(salon.getAcceptsOnlineBooking())
                .currentQueue(currentQueue)
                .nextAvailableTime(nextAvailable)
                .estimatedWaitMinutes(Math.max(0, estimatedWaitMinutes))
                .availableBarbers(barberRepository.findBySalonIdAndIsAvailableTrue(salonId).size())
                .build();
    }

    @Transactional
    public void toggleSalonStatus(UUID salonId, Boolean isOpen) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));
        salon.setIsOpen(isOpen);
        salonRepository.save(salon);
    }

    @Transactional
    public ServiceResponse addService(UUID salonId, CreateServiceRequest request, UUID ownerId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        if (!salon.getOwner().getId().equals(ownerId)) {
            throw new BusinessException("You don't have permission");
        }

        com.salonplatform.entity.Service service = com.salonplatform.entity.Service.builder()
                .salon(salon)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .category(request.getCategory())
                .isActive(true)
                .build();

        service = serviceRepository.save(service);
        return toServiceResponse(service);
    }

    @Transactional
    public BarberResponse addBarber(UUID salonId, CreateBarberRequest request, UUID ownerId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        if (!salon.getOwner().getId().equals(ownerId)) {
            throw new BusinessException("You don't have permission");
        }

        Barber barber = Barber.builder()
                .salon(salon)
                .name(request.getName())
                .phone(request.getPhone())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .rating(BigDecimal.ZERO)
                .totalServices(0)
                .isAvailable(true)
                .build();

        barber = barberRepository.save(barber);
        return toBarberResponse(barber);
    }

    @Transactional(readOnly = true)
    public List<SalonResponse> getOwnedSalons(UUID ownerId) {
        return salonRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::toSalonResponse)
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateNextAvailableTime(UUID salonId) {
        List<Queue> queue = queueRepository.findBySalonIdAndStatusOrderByPositionAsc(
                salonId, QueueStatus.WAITING);

        if (queue.isEmpty()) return LocalDateTime.now();

        int totalWaitMinutes = queue.stream()
                .mapToInt(q -> q.getBooking().getEstimatedDurationMinutes())
                .sum();

        return LocalDateTime.now().plusMinutes(totalWaitMinutes);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("Coordinates required");
        }
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new BusinessException("Invalid coordinates");
        }
    }

    private SalonResponse toSalonResponse(Salon salon) {
        return SalonResponse.builder()
                .id(salon.getId())
                .name(salon.getName())
                .address(salon.getAddress())
                .city(salon.getCity())
                .latitude(salon.getLatitude())
                .longitude(salon.getLongitude())
                .phone(salon.getPhone())
                .rating(salon.getRating())
                .totalReviews(salon.getTotalReviews())
                .isOpen(salon.isOpenNow())
                .openingTime(salon.getOpeningTime())
                .closingTime(salon.getClosingTime())
                .build();
    }

    private SalonResponse toSalonResponseWithAvailability(Salon salon) {
        long queueCount = queueRepository.countWaitingCustomers(salon.getId());
        LocalDateTime nextAvailable = calculateNextAvailableTime(salon.getId());
        int estimatedWaitMinutes = nextAvailable != null
                ? (int) java.time.Duration.between(LocalDateTime.now(), nextAvailable).toMinutes()
                : 0;

        return SalonResponse.builder()
                .id(salon.getId())
                .name(salon.getName())
                .address(salon.getAddress())
                .city(salon.getCity())
                .latitude(salon.getLatitude())
                .longitude(salon.getLongitude())
                .phone(salon.getPhone())
                .rating(salon.getRating())
                .totalReviews(salon.getTotalReviews())
                .isOpen(salon.isOpenNow())
                .currentQueue(queueCount)
                .nextAvailableTime(nextAvailable)
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .build();
    }

    private ServiceResponse toServiceResponse(com.salonplatform.entity.Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .category(service.getCategory().name())
                .isActive(service.getIsActive())
                .build();
    }

    private BarberResponse toBarberResponse(Barber barber) {
        return BarberResponse.builder()
                .id(barber.getId())
                .name(barber.getName())
                .phone(barber.getPhone())
                .specialization(barber.getSpecialization())
                .experienceYears(barber.getExperienceYears())
                .rating(barber.getRating())
                .totalServices(barber.getTotalServices())
                .isAvailable(barber.getIsAvailable())
                .build();
    }
}