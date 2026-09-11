package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.*;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.*;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BriefingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private VisitorRepository visitorRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private BriefingRepository briefingRepository;
    @Mock
    private SchedulingService schedulingService;
    @InjectMocks
    private RegistrationServiceImpl service;

    private static final List<RegistrationStatus> INACTIVE =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    private Visitor visitor;
    private TimeSlot slot;
    private Registration registration;

    @BeforeEach
    void setUp() {
        visitor = new Visitor();
        visitor.setId(1L);
        visitor.setFullName("Alice Dupont");
        visitor.setGroupSize(2);
        visitor.setEmail("alice@test.com");
        visitor.setType(VisitorType.INDIVIDUAL);

        slot = new TimeSlot();
        slot.setId(10L);
        slot.setDate(LocalDate.of(2026, 9, 8));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setMaxCapacity(10);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot.setRegistrations(new java.util.ArrayList<>());

        registration = new Registration();
        registration.setId(100L);
        registration.setVisitor(visitor);
        registration.setTimeSlot(slot);
        registration.setStatus(RegistrationStatus.PENDING);
    }

    // --- Visitors ---

    @Test
    void createVisitor_duplicateEmail_throws() {
        when(visitorRepository.existsByEmail("alice@test.com")).thenReturn(true);

        VisitorRequest req = new VisitorRequest();
        req.setFullName("Alice");
        req.setType(VisitorType.INDIVIDUAL);
        req.setEmail("alice@test.com");

        assertThatThrownBy(() -> service.createVisitor(req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createVisitor_success() {
        when(visitorRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(inv -> {
            Visitor v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        VisitorRequest req = new VisitorRequest();
        req.setFullName("Alice");
        req.setType(VisitorType.INDIVIDUAL);
        req.setEmail("alice@test.com");

        VisitorResponse resp = service.createVisitor(req);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getFullName()).isEqualTo("Alice");
    }

    // --- Register ---

    @Test
    void register_cancelledSlot_throws() {
        slot.setStatus(TimeSlotStatus.CANCELLED);
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.TOURISM);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void register_duplicate_throws() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(true);

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.TOURISM);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_noCapacity_throws() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(9L);

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.TOURISM);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void register_success() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(2L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.TOURISM);

        RegistrationResponse resp = service.register(req);
        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(resp.getVisitPurpose()).isEqualTo(VisitPurpose.TOURISM);
        assertThat(resp.isProspect()).isFalse();
        verify(schedulingService).refreshStatus(slot);
    }

    @Test
    void register_withPurchasePurpose_createsProspect() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(2L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.PURCHASE);

        RegistrationResponse resp = service.register(req);
        assertThat(resp.getVisitPurpose()).isEqualTo(VisitPurpose.PURCHASE);
        assertThat(resp.isProspect()).isTrue();
    }

    @Test
    void register_withPartnershipPurpose_createsProspect() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(2L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.PARTNERSHIP);

        RegistrationResponse resp = service.register(req);
        assertThat(resp.getVisitPurpose()).isEqualTo(VisitPurpose.PARTNERSHIP);
        assertThat(resp.isProspect()).isTrue();
    }

    @Test
    void register_withInvestmentPurpose_createsProspect() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(2L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.INVESTMENT);

        RegistrationResponse resp = service.register(req);
        assertThat(resp.getVisitPurpose()).isEqualTo(VisitPurpose.INVESTMENT);
        assertThat(resp.isProspect()).isTrue();
    }

    // --- Approve ---

    @Test
    void approve_notPending_throws() {
        registration.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.approve(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void approve_success_createsBriefing() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(briefingRepository.save(any(Briefing.class))).thenAnswer(inv -> {
            Briefing b = inv.getArgument(0);
            b.setId(200L);
            return b;
        });
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse resp = service.approve(100L);
        assertThat(resp.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        verify(briefingRepository).save(any(Briefing.class));
    }

    // --- Reject ---

    @Test
    void reject_notPending_throws() {
        registration.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.reject(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void reject_success() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse resp = service.reject(100L);
        assertThat(resp.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    }

    // --- Check-in ---

    @Test
    void checkIn_notConfirmed_throws() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.checkIn(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void checkIn_success() {
        registration.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse resp = service.checkIn(100L);
        assertThat(resp.getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
    }

    // --- Cancel ---

    @Test
    void cancel_alreadyCheckedIn_throws() {
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.cancel(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    void cancel_success() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse resp = service.cancel(100L);
        assertThat(resp.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    }

    // --- Briefing ---

    @Test
    void getBriefing_notConfirmed_throws() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.getBriefingForRegistration(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not confirmed");
    }

    @Test
    void deliverBriefing_noBriefing_throws() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));

        BriefingDeliverRequest req = new BriefingDeliverRequest();
        req.setStaffMember("Staff");

        assertThatThrownBy(() -> service.deliverBriefing(100L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No briefing");
    }

    @Test
    void deliverBriefing_success() {
        Briefing briefing = new Briefing();
        briefing.setId(200L);
        briefing.setRegistration(registration);
        briefing.setStatus(BriefingStatus.PENDING);
        registration.setBriefing(briefing);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(briefingRepository.save(any(Briefing.class))).thenAnswer(inv -> inv.getArgument(0));

        BriefingDeliverRequest req = new BriefingDeliverRequest();
        req.setStaffMember("Guard Camille");
        req.setSignature("sig123");

        BriefingResponse resp = service.deliverBriefing(100L, req);
        assertThat(resp.getStatus()).isEqualTo(BriefingStatus.DONE);
        assertThat(resp.getStaffMember()).isEqualTo("Guard Camille");
    }

    // --- RefreshStatus interaction ---

    @Test
    void register_then_approve_refreshesStatus() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(visitor));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(registrationRepository.existsByVisitorIdAndTimeSlotId(1L, 10L)).thenReturn(false);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(10L, INACTIVE)).thenReturn(0L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequest req = new RegistrationRequest();
        req.setVisitorId(1L);
        req.setTimeSlotId(10L);
        req.setVisitPurpose(VisitPurpose.TOURISM);
        service.register(req);
        verify(schedulingService).refreshStatus(slot);
    }

    // --- Prospects ---

    @Test
    void getProspects_delegatesToRepository() {
        Registration prospect = new Registration();
        prospect.setId(200L);
        prospect.setVisitor(visitor);
        prospect.setTimeSlot(slot);
        prospect.setVisitPurpose(VisitPurpose.PURCHASE);
        prospect.setProspect(true);
        prospect.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByIsProspectTrue()).thenReturn(List.of(prospect));

        List<RegistrationResponse> result = service.getProspects();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isProspect()).isTrue();
        assertThat(result.get(0).getVisitPurpose()).isEqualTo(VisitPurpose.PURCHASE);
    }
}
