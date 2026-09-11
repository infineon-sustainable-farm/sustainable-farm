package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingOccupancyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.AgriActivityRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final List<BookingStatus> INACTIVE_STATUSES =
            List.of(BookingStatus.CANCELLED);

    private final BookingRepository bookingRepository;
    private final AgriActivityRepository activityRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final NotificationService notificationService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              AgriActivityRepository activityRepository,
                              TimeSlotRepository timeSlotRepository,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.activityRepository = activityRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgriActivityResponse> listActivities() {
        return activityRepository.findAllByOrderByNameAsc().stream()
                .map(AgriActivityResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AgriActivityResponse createActivity(AgriActivityRequest request) {
        AgriActivity activity = new AgriActivity();
        apply(activity, request);
        return AgriActivityResponse.from(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public AgriActivityResponse updateActivity(Long id, AgriActivityRequest request) {
        AgriActivity activity = getActivityEntity(id);
        apply(activity, request);
        return AgriActivityResponse.from(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public void deactivateActivity(Long id) {
        AgriActivity activity = getActivityEntity(id);
        activity.setActive(false);
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> listBookings(BookingStatus status, Long activityId, LocalDate date) {
        List<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByStatus(status);
        } else if (activityId != null) {
            bookings = bookingRepository.findByActivityId(activityId);
        } else if (date != null) {
            bookings = bookingRepository.findByTimeSlotDate(date);
        } else {
            bookings = bookingRepository.findAllByOrderByIdDesc();
        }
        return bookings.stream()
                .map(BookingResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        AgriActivity activity = getActiveActivity(request.getActivityId());
        TimeSlot slot = getAvailableSlot(request.getTimeSlotId());
        long occupied = countOccupied(request.getActivityId(), request.getTimeSlotId());
        validateCapacity(activity, slot.getMaxCapacity(), occupied, request.getPeopleCount());

        Booking booking = new Booking();
        apply(booking, request, activity, slot);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = getBookingEntity(id);
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot update a completed booking " + id);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled booking " + id);
        }
        AgriActivity activity = getActiveActivity(request.getActivityId());
        TimeSlot slot = getAvailableSlot(request.getTimeSlotId());
        long occupied = countOccupied(request.getActivityId(), request.getTimeSlotId())
                - booking.getPeopleCount();
        validateCapacity(activity, slot.getMaxCapacity(), occupied, request.getPeopleCount());

        apply(booking, request, activity, slot);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse markPaid(Long id) {
        Booking booking = getBookingEntity(id);
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot record payment on a "
                    + booking.getStatus().name().toLowerCase() + " booking " + id);
        }
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        Booking booking = getBookingEntity(id);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING bookings can be confirmed (current: "
                    + booking.getStatus() + ")");
        }
        if (booking.getPaymentStatus() != BookingPaymentStatus.PAID) {
            throw new BusinessRuleException("Booking " + id + " must be paid before confirmation");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setReminderScheduledAt(booking.getTimeSlot().getDate()
                .atTime(booking.getTimeSlot().getStartTime())
                .minusHours(24)
                .toInstant(ZoneOffset.UTC));
        sendConfirmation(booking);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    private void sendConfirmation(Booking booking) {
        try {
            BookingEmailBuilder.MailContent mail = BookingEmailBuilder.confirmation(booking);
            notificationService.send(booking.getVisitorEmail(), mail.subject(), mail.html());
            booking.setConfirmationSentAt(Instant.now());
        } catch (Exception e) {
            log.warn("Could not send confirmation email for booking {}: {}",
                    booking.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long id) {
        Booking booking = getBookingEntity(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED bookings can be completed (current: "
                    + booking.getStatus() + ")");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = getBookingEntity(id);
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only PENDING or CONFIRMED bookings can be cancelled "
                    + "(current: " + booking.getStatus() + ")");
        }
        if (booking.getPaymentStatus() == BookingPaymentStatus.PAID) {
            booking.setPaymentStatus(BookingPaymentStatus.REFUNDED);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingOccupancyResponse> getOccupancy() {
        Map<Long, Long> sums = new HashMap<>();
        for (Object[] row : bookingRepository.sumPeopleCountGroupedByActivity(INACTIVE_STATUSES)) {
            sums.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return activityRepository.findAll().stream()
                .map(activity -> {
                    BookingOccupancyResponse r = new BookingOccupancyResponse();
                    r.setActivityId(activity.getId());
                    r.setActivityName(activity.getName());
                    r.setCapacity(activity.getCapacity());
                    long booked = sums.getOrDefault(activity.getId(), 0L);
                    r.setBooked((int) booked);
                    r.setOccupancyPercent(activity.getCapacity() > 0
                            ? (int) Math.round(booked * 100.0 / activity.getCapacity())
                            : 0);
                    return r;
                })
                .collect(Collectors.toList());
    }

    private void apply(AgriActivity activity, AgriActivityRequest request) {
        activity.setName(request.getName());
        activity.setPrice(request.getPrice());
        activity.setCapacity(request.getCapacity());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setDescription(request.getDescription());
    }

    private void apply(Booking booking, BookingRequest request,
                       AgriActivity activity, TimeSlot slot) {
        booking.setActivity(activity);
        booking.setTimeSlot(slot);
        booking.setVisitorFullName(request.getVisitorFullName());
        booking.setVisitorEmail(request.getVisitorEmail());
        booking.setVisitorPhone(request.getVisitorPhone());
        booking.setPeopleCount(request.getPeopleCount());
        booking.setPaymentMethod(request.getPaymentMethod());
    }

    private AgriActivity getActivityEntity(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity " + id + " not found"));
    }

    private AgriActivity getActiveActivity(Long id) {
        AgriActivity activity = getActivityEntity(id);
        if (!activity.isActive()) {
            throw new BusinessRuleException("Activity " + id + " is not active");
        }
        return activity;
    }

    private TimeSlot getAvailableSlot(Long id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot " + id + " not found"));
        if (slot.getStatus() == TimeSlotStatus.CANCELLED) {
            throw new BusinessRuleException("TimeSlot " + id + " is cancelled");
        }
        return slot;
    }

    private Booking getBookingEntity(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " not found"));
    }

    private long countOccupied(Long activityId, Long timeSlotId) {
        return bookingRepository.sumPeopleCountByActivityAndSlot(
                activityId, timeSlotId, INACTIVE_STATUSES);
    }

    private void validateCapacity(AgriActivity activity, int slotMaxCapacity,
                                  long occupied, int peopleCount) {
        if (peopleCount > slotMaxCapacity) {
            throw new ConflictException("Booking exceeds the slot capacity of "
                    + slotMaxCapacity + " people");
        }
        if (occupied + peopleCount > activity.getCapacity()) {
            throw new ConflictException("Not enough remaining capacity on activity "
                    + activity.getId() + " (max " + activity.getCapacity()
                    + ", already booked " + occupied + ")");
        }
    }
}