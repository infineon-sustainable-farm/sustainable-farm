package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AvailabilityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class SchedulingServiceImpl implements SchedulingService {

    private static final List<RegistrationStatus> INACTIVE_STATUSES =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    private static final List<RegistrationStatus> ACTIVE_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED,
                    RegistrationStatus.CHECKED_IN);

    /**
     * Number of write locks used to serialise slot creation and update per
     * date. A fixed stripe count keeps the structure bounded: two dates
     * occasionally sharing a stripe only costs a little waiting, never
     * correctness.
     */
    private static final int DATE_LOCK_STRIPES = 64;

    private final TimeSlotRepository timeSlotRepository;
    private final RegistrationRepository registrationRepository;
    private final BookingService bookingService;
    private final StaffRepository staffRepository;
    private final ReentrantLock[] dateLocks = createDateLocks();
    private final TransactionTemplate transactionTemplate;

    public SchedulingServiceImpl(TimeSlotRepository timeSlotRepository,
                                 RegistrationRepository registrationRepository,
                                 BookingService bookingService,
                                 StaffRepository staffRepository,
                                 PlatformTransactionManager transactionManager) {
        this.timeSlotRepository = timeSlotRepository;
        this.registrationRepository = registrationRepository;
        this.bookingService = bookingService;
        this.staffRepository = staffRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private static ReentrantLock[] createDateLocks() {
        ReentrantLock[] locks = new ReentrantLock[DATE_LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        return locks;
    }

    /**
     * Runs a slot write while holding the date's stripe lock.
     *
     * The overlap check and the insert must be one atomic step per date:
     * without the lock, two concurrent requests both pass the check and create
     * overlapping slots. The lock alone is not enough either — with a
     * method-level @Transactional the commit happens after the lock is
     * released, so the next request would not see the pending row yet. The
     * transaction is therefore opened programmatically inside the lock, which
     * keeps check, insert and commit in the same critical section.
     *
     * This protects a single application instance, the deployment model of
     * this farm app.
     */
    private <T> T withDateLock(LocalDate date, Supplier<T> action) {
        ReentrantLock lock = dateLocks[Math.floorMod(date.hashCode(), DATE_LOCK_STRIPES)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
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
    public Page<TimeSlotResponse> findAll(int page, int size) {
        return timeSlotRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "slotDate", "startTime")))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSlotResponse findById(Long id) {
        return toResponse(getSlot(id));
    }

    @Override
    public TimeSlotResponse create(TimeSlotRequest request) {
        return withDateLock(request.getDate(), () -> transactionTemplate.execute(status -> {
            validateCommon(request);
            validateNotInPast(request.getDate());
            if (timeSlotRepository.existsOverlapping(
                    request.getDate(), request.getStartTime(), request.getEndTime())) {
                throw new ConflictException(
                        "A time slot already overlaps " + request.getDate() + " between "
                                + request.getStartTime() + " and " + request.getEndTime());
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
        }));
    }

    @Override
    public TimeSlotResponse update(Long id, TimeSlotRequest request) {
        return withDateLock(request.getDate(), () -> transactionTemplate.execute(status -> {
            TimeSlot slot = getSlotForUpdate(id);
            if (slot.getStatus() == TimeSlotStatus.CANCELLED) {
                throw new BusinessRuleException("Cannot update a cancelled time slot " + id);
            }
            validateCommon(request);
            if (timeSlotRepository.existsOverlappingExcluding(
                    request.getDate(), request.getStartTime(), request.getEndTime(), id)) {
                throw new ConflictException(
                        "A time slot already overlaps " + request.getDate() + " between "
                                + request.getStartTime() + " and " + request.getEndTime()
                                + " (excluding " + id + ")");
            }
            if (slot.getStatus() == TimeSlotStatus.COMPLETED) {
                throw new BusinessRuleException("Cannot update a completed time slot " + id);
            }
            apply(slot, request);
            refreshStatus(slot);
            timeSlotRepository.save(slot);
            timeSlotRepository.flush();
            return toResponse(slot);
        }));
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        TimeSlot slot = getSlotForUpdate(id);
        if (slot.getStatus() == TimeSlotStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed time slot " + id);
        }
        if (slot.getStatus() == TimeSlotStatus.CANCELLED) {
            return;
        }
        slot.setStatus(TimeSlotStatus.CANCELLED);
        timeSlotRepository.save(slot);

        List<Registration> registrations = registrationRepository
                .findByTimeSlotIdAndStatusNotIn(id, INACTIVE_STATUSES);
        for (Registration registration : registrations) {
            registration.setStatus(RegistrationStatus.CANCELLED);
        }
        registrationRepository.saveAll(registrations);

        bookingService.cancelBookingsForSlot(id);
    }

    @Override
    @Transactional
    public TimeSlotResponse assignGuide(Long id, Long guideId) {
        TimeSlot slot = getSlotForUpdate(id);
        if (guideId == null) {
            throw new BusinessRuleException("guideId is required");
        }
        slot.setGuide(resolveGuide(guideId));
        timeSlotRepository.save(slot);
        timeSlotRepository.flush();
        return toResponse(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(LocalDate date) {
        LocalDate today = LocalDate.now();
        return timeSlotRepository.findByDate(date).stream()
                .filter(s -> s.getStatus() != TimeSlotStatus.CANCELLED)
                .filter(s -> !s.getDate().isBefore(today))
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
        return registrationRepository.sumGroupSizeByTimeSlotIdAndStatusNotIn(slotId, INACTIVE_STATUSES);
    }

    private void validateCommon(TimeSlotRequest request) {
        if (request.getDate() == null) {
            throw new BusinessRuleException("date is required");
        }
        if (request.getDate().getDayOfWeek() == SchedulingRules.CLOSED_DAY) {
            throw new BusinessRuleException(
                    "Farm is closed on " + SchedulingRules.CLOSED_DAY + " — no slots allowed on "
                            + request.getDate());
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessRuleException("endTime must be after startTime");
        }
    }

    private void validateNotInPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Cannot create a time slot in the past: " + date);
        }
    }

    private void apply(TimeSlot slot, TimeSlotRequest request) {
        slot.setDate(request.getDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setMaxCapacity(request.getMaxCapacity() == null ? 10 : request.getMaxCapacity());
        if (request.getGuideId() != null) {
            slot.setGuide(resolveGuide(request.getGuideId()));
        }
    }

    private Staff resolveGuide(Long guideId) {
        return staffRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member " + guideId + " not found"));
    }

    private TimeSlot getSlot(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot " + id + " not found"));
    }

    private TimeSlot getSlotForUpdate(Long id) {
        return timeSlotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot " + id + " not found"));
    }

    private TimeSlotResponse toResponse(TimeSlot slot) {
        return TimeSlotResponse.from(slot, countBooked(slot.getId()));
    }
}
