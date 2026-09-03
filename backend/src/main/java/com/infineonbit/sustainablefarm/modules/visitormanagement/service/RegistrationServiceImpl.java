package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingDeliverRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Briefing;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BriefingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BriefingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final List<RegistrationStatus> INACTIVE_STATUSES =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    private final VisitorRepository visitorRepository;
    private final RegistrationRepository registrationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BriefingRepository briefingRepository;
    private final SchedulingService schedulingService;

    public RegistrationServiceImpl(VisitorRepository visitorRepository,
                                   RegistrationRepository registrationRepository,
                                   TimeSlotRepository timeSlotRepository,
                                   BriefingRepository briefingRepository,
                                   SchedulingService schedulingService) {
        this.visitorRepository = visitorRepository;
        this.registrationRepository = registrationRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.briefingRepository = briefingRepository;
        this.schedulingService = schedulingService;
    }

    // ---------------- Visitors ----------------

    @Override
    @Transactional
    public VisitorResponse createVisitor(VisitorRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && visitorRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("A visitor with email " + request.getEmail() + " already exists");
        }
        Visitor visitor = new Visitor();
        apply(visitor, request);
        return VisitorResponse.from(visitorRepository.save(visitor));
    }

    @Override
    @Transactional
    public VisitorResponse updateVisitor(Long id, VisitorRequest request) {
        Visitor visitor = getVisitorEntity(id);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            visitorRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already used by visitor " + existing.getId());
                    });
        }
        apply(visitor, request);
        return VisitorResponse.from(visitorRepository.save(visitor));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorResponse getVisitor(Long id) {
        return VisitorResponse.from(getVisitorEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponse> listVisitors() {
        return visitorRepository.findAll().stream()
                .map(VisitorResponse::from)
                .collect(Collectors.toList());
    }

    // ---------------- Registrations ----------------

    @Override
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        Visitor visitor = getVisitorEntity(request.getVisitorId());
        TimeSlot slot = getSlotEntity(request.getTimeSlotId());

        if (slot.getStatus() == TimeSlotStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot register on a cancelled time slot " + slot.getId());
        }
        if (registrationRepository.existsByVisitorIdAndTimeSlotId(visitor.getId(), slot.getId())) {
            throw new ConflictException("Visitor " + visitor.getId()
                    + " is already registered on time slot " + slot.getId());
        }

        long booked = registrationRepository
                .countByTimeSlotIdAndStatusNotIn(slot.getId(), INACTIVE_STATUSES);
        if (booked + visitor.getGroupSize() > slot.getMaxCapacity()) {
            throw new BusinessRuleException("Not enough capacity on time slot " + slot.getId()
                    + " (max " + slot.getMaxCapacity() + ")");
        }

        Registration registration = new Registration();
        registration.setVisitor(visitor);
        registration.setTimeSlot(slot);
        registration.setEventId(request.getEventId());
        registration.setStatus(RegistrationStatus.PENDING);
        Registration saved = registrationRepository.save(registration);

        // Add to the owning slot's collection and recompute its status.
        slot.getRegistrations().add(saved);
        schedulingService.refreshStatus(slot);
        timeSlotRepository.save(slot);
        return RegistrationResponse.from(saved);
    }

    @Override
    @Transactional
    public RegistrationResponse approve(Long registrationId) {
        Registration registration = getRegistrationEntity(registrationId);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING registrations can be approved (current: "
                    + registration.getStatus() + ")");
        }
        registration.setStatus(RegistrationStatus.CONFIRMED);

        // A confirmed registration automatically triggers a safety briefing (Safety link).
        if (registration.getBriefing() == null) {
            Briefing briefing = new Briefing();
            briefing.setRegistration(registration);
            briefing.setStatus(BriefingStatus.PENDING);
            briefingRepository.save(briefing);
            registration.setBriefing(briefing);
        }
        schedulingService.refreshStatus(registration.getTimeSlot());
        timeSlotRepository.save(registration.getTimeSlot());
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public RegistrationResponse reject(Long registrationId) {
        Registration registration = getRegistrationEntity(registrationId);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING registrations can be rejected (current: "
                    + registration.getStatus() + ")");
        }
        registration.setStatus(RegistrationStatus.REJECTED);
        registrationRepository.save(registration);
        schedulingService.refreshStatus(registration.getTimeSlot());
        timeSlotRepository.save(registration.getTimeSlot());
        return RegistrationResponse.from(registration);
    }

    @Override
    @Transactional
    public RegistrationResponse checkIn(Long registrationId) {
        Registration registration = getRegistrationEntity(registrationId);
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED registrations can check in (current: "
                    + registration.getStatus() + ")");
        }
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long registrationId) {
        Registration registration = getRegistrationEntity(registrationId);
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new BusinessRuleException("Cannot cancel registration " + registrationId
                    + ": visitor has already checked in");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        schedulingService.refreshStatus(registration.getTimeSlot());
        timeSlotRepository.save(registration.getTimeSlot());
        return RegistrationResponse.from(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponse getRegistration(Long id) {
        return RegistrationResponse.from(getRegistrationEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrationsBySlot(Long timeSlotId) {
        return registrationRepository.findByTimeSlotId(timeSlotId).stream()
                .map(RegistrationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrationsByDate(LocalDate date) {
        return registrationRepository.findAllByTimeSlotDate(date).stream()
                .map(RegistrationResponse::from)
                .collect(Collectors.toList());
    }

    // ---------------- Briefing ----------------

    @Override
    @Transactional(readOnly = true)
    public BriefingResponse getBriefingForRegistration(Long registrationId) {
        Briefing briefing = getRegistrationEntity(registrationId).getBriefing();
        if (briefing == null) {
            throw new ResourceNotFoundException(
                    "No briefing for registration " + registrationId + " (not confirmed)");
        }
        return BriefingResponse.from(briefing);
    }

    @Override
    @Transactional
    public BriefingResponse deliverBriefing(Long registrationId, BriefingDeliverRequest request) {
        Registration registration = getRegistrationEntity(registrationId);
        Briefing briefing = registration.getBriefing();
        if (briefing == null) {
            throw new BusinessRuleException("No briefing to deliver for registration " + registrationId);
        }
        briefing.setStaffMember(request.getStaffMember());
        briefing.setSignature(request.getSignature());
        briefing.setDeliveredAt(Instant.now());
        briefing.setStatus(BriefingStatus.DONE);
        return BriefingResponse.from(briefingRepository.save(briefing));
    }

    // ---------------- Helpers ----------------

    private void apply(Visitor visitor, VisitorRequest request) {
        visitor.setFullName(request.getFullName());
        visitor.setGroupSize(request.getGroupSize() == null ? 1 : request.getGroupSize());
        visitor.setEmail(request.getEmail());
        visitor.setPhone(request.getPhone());
        visitor.setLanguage(request.getLanguage());
        visitor.setType(request.getType() == null ? VisitorType.INDIVIDUAL : request.getType());
        visitor.setSpecialNeeds(request.getSpecialNeeds());
    }

    private Visitor getVisitorEntity(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor " + id + " not found"));
    }

    private TimeSlot getSlotEntity(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot " + id + " not found"));
    }

    private Registration getRegistrationEntity(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration " + id + " not found"));
    }
}
