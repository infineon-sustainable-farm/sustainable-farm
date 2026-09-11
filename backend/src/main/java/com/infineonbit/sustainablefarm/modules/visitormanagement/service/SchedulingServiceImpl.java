package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AvailabilityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchedulingServiceImpl implements SchedulingService {

    private static final List<RegistrationStatus> INACTIVE_STATUSES =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    private final TimeSlotRepository timeSlotRepository;
    private final RegistrationRepository registrationRepository;

    public SchedulingServiceImpl(TimeSlotRepository timeSlotRepository,
                                 RegistrationRepository registrationRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findByDate(LocalDate date) {
        return timeSlotRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findAll() {
        return timeSlotRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSlotResponse findById(Long id) {
        return toResponse(getSlot(id));
    }

    @Override
    @Transactional
    public TimeSlotResponse create(TimeSlotRequest request) {
        validateCommon(request);
        if (timeSlotRepository.existsByDateAndStartTime(request.getDate(), request.getStartTime())) {
            throw new ConflictException(
                    "A time slot already exists on " + request.getDate() + " at " + request.getStartTime());
        }
        long slotsThatDay = timeSlotRepository.countByDateAndStatusNot(request.getDate(), TimeSlotStatus.CANCELLED);
        if (slotsThatDay >= SchedulingRules.MAX_SLOTS_PER_DAY) {
            throw new BusinessRuleException(
                    "Maximum of " + SchedulingRules.MAX_SLOTS_PER_DAY + " slots per day reached for "
                            + request.getDate());
        }
        TimeSlot slot = new TimeSlot();
        apply(slot, request);
        return toResponse(timeSlotRepository.save(slot));
    }

    @Override
    @Transactional
    public TimeSlotResponse update(Long id, TimeSlotRequest request) {
        TimeSlot slot = getSlot(id);
        if (slot.getStatus() == TimeSlotStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled time slot " + id);
        }
        validateCommon(request);
        if (slot.getStatus() == TimeSlotStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot update a completed time slot " + id);
        }
        apply(slot, request);
        refreshStatus(slot);
        return toResponse(timeSlotRepository.save(slot));
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        TimeSlot slot = getSlot(id);
        if (slot.getStatus() == TimeSlotStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed time slot " + id);
        }
        slot.setStatus(TimeSlotStatus.CANCELLED);
        timeSlotRepository.save(slot);
    }

    @Override
    @Transactional
    public TimeSlotResponse assignGuide(Long id, Long guideId) {
        TimeSlot slot = getSlot(id);
        if (guideId == null) {
            throw new BusinessRuleException("guideId is required");
        }
        slot.setGuideId(guideId);
        return toResponse(timeSlotRepository.save(slot));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(LocalDate date) {
        return timeSlotRepository.findByDate(date).stream()
                .filter(s -> s.getStatus() != TimeSlotStatus.CANCELLED)
                .map(s -> {
                    long booked = countBooked(s.getId());
                    int remaining = Math.max(0, s.getMaxCapacity() - (int) booked);
                    return new AvailabilityResponse(
                            s.getId(), s.getDate(), s.getStartTime(), s.getEndTime(),
                            s.getMaxCapacity(), remaining,
                            s.getStatus() == TimeSlotStatus.FULL ? s.getStatus()
                                    : (booked > 0 ? TimeSlotStatus.RESERVED : s.getStatus()),
                            s.getGuideId());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void refreshStatus(TimeSlot slot) {
        long booked = countBooked(slot.getId());
        if (booked >= slot.getMaxCapacity()) {
            slot.setStatus(TimeSlotStatus.FULL);
        } else if (booked > 0) {
            slot.setStatus(TimeSlotStatus.RESERVED);
        } else {
            slot.setStatus(TimeSlotStatus.AVAILABLE);
        }
    }

    private long countBooked(Long slotId) {
        return registrationRepository.countByTimeSlotIdAndStatusNotIn(slotId, INACTIVE_STATUSES);
    }

    private void validateCommon(TimeSlotRequest request) {
        if (request.getDate().getDayOfWeek() == SchedulingRules.CLOSED_DAY) {
            throw new BusinessRuleException(
                    "Farm is closed on " + SchedulingRules.CLOSED_DAY + " — no slots allowed on "
                            + request.getDate());
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessRuleException("endTime must be after startTime");
        }
        int capacity = request.getMaxCapacity() == null ? 10 : request.getMaxCapacity();
        if (capacity > SchedulingRules.MAX_VISITORS_PER_SLOT) {
            throw new BusinessRuleException(
                    "maxCapacity cannot exceed " + SchedulingRules.MAX_VISITORS_PER_SLOT);
        }
    }

    private void apply(TimeSlot slot, TimeSlotRequest request) {
        slot.setDate(request.getDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setMaxCapacity(request.getMaxCapacity() == null ? 10 : request.getMaxCapacity());
        slot.setGuideId(request.getGuideId());
    }

    private TimeSlot getSlot(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot " + id + " not found"));
    }

    private TimeSlotResponse toResponse(TimeSlot slot) {
        return TimeSlotResponse.from(slot, countBooked(slot.getId()));
    }
}
