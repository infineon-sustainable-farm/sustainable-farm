package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private VisitorRepository visitorRepository;
    @InjectMocks
    private EventServiceImpl service;

    private Long eventId = 1L;
    private LocalDateTime start = LocalDateTime.of(2026, 10, 4, 10, 0);
    private LocalDateTime end = LocalDateTime.of(2026, 10, 4, 16, 0);

    @BeforeEach
    void setUp() {
    }

    private Event buildEvent(EventStatus status) {
        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Open Farm Day");
        event.setType(EventType.OPEN_DAY);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setMaxCapacity(60);
        event.setStatus(status);
        return event;
    }

    private EventRequest buildRequest() {
        EventRequest req = new EventRequest();
        req.setTitle("Open Farm Day");
        req.setType(EventType.OPEN_DAY);
        req.setStartDateTime(start);
        req.setEndDateTime(end);
        req.setMaxCapacity(60);
        return req;
    }

    private EventRegistrationRequest buildRegistrationRequest() {
        EventRegistrationRequest req = new EventRegistrationRequest();
        req.setVisitorId(10L);
        req.setVisitPurpose(VisitPurpose.PURCHASE);
        return req;
    }

    @Test
    void createEvent_success() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = service.createEvent(buildRequest());

        assertThat(response.getTitle()).isEqualTo("Open Farm Day");
        assertThat(response.getType()).isEqualTo(EventType.OPEN_DAY);
        assertThat(response.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.getBooked()).isZero();
    }

    @Test
    void createEvent_endBeforeStart_throws() {
        EventRequest req = buildRequest();
        req.setEndDateTime(start.minusHours(1));

        assertThatThrownBy(() -> service.createEvent(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("endDateTime");
    }

    @Test
    void getEvent_notFound_throws() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateEvent_cancelled_throws() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.CANCELLED)));

        assertThatThrownBy(() -> service.updateEvent(eventId, buildRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void updateEvent_success() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.DRAFT)));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventIdAndStatusNotIn(eq(eventId), any())).thenReturn(3L);

        EventRequest req = buildRequest();
        req.setTitle("Updated Title");
        EventResponse response = service.updateEvent(eventId, req);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getBooked()).isEqualTo(3L);
    }

    @Test
    void publishEvent_draft_success() {
        Event event = buildEvent(EventStatus.DRAFT);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventIdAndStatusNotIn(eq(eventId), any())).thenReturn(0L);

        EventResponse response = service.publishEvent(eventId);

        assertThat(response.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void publishEvent_alreadyPublished_throws() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.publishEvent(eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void publishEvent_pastEvent_throws() {
        Event event = buildEvent(EventStatus.DRAFT);
        event.setEndDateTime(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publishEvent(eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ended");
    }

    @Test
    void cancelEvent_success() {
        Event event = buildEvent(EventStatus.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancelEvent(eventId);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void cancelEvent_completed_throws() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.COMPLETED)));

        assertThatThrownBy(() -> service.cancelEvent(eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void getRegistrations_returnsEventRegistrations() {
        Registration reg = new Registration();
        reg.setId(5L);
        Visitor visitor = new Visitor();
        visitor.setId(10L);
        reg.setVisitor(visitor);
        reg.setEventId(eventId);
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setVisitPurpose(VisitPurpose.PURCHASE);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(registrationRepository.findByEventId(eventId)).thenReturn(List.of(reg));

        List<RegistrationResponse> responses = service.getRegistrations(eventId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEventId()).isEqualTo(eventId);
        assertThat(responses.get(0).getVisitorId()).isEqualTo(10L);
        assertThat(responses.get(0).getTimeSlotId()).isNull();
    }

    @Test
    void registerVisitor_success() {
        Visitor visitor = new Visitor();
        visitor.setId(10L);
        visitor.setFullName("Test Visitor");
        visitor.setGroupSize(2);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(visitorRepository.findById(10L)).thenReturn(Optional.of(visitor));
        when(registrationRepository.existsByEventIdAndVisitorId(eventId, 10L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatusNotIn(eq(eventId), any())).thenReturn(4L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse response = service.registerVisitor(eventId, buildRegistrationRequest());

        assertThat(response.getVisitorId()).isEqualTo(10L);
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getVisitPurpose()).isEqualTo(VisitPurpose.PURCHASE);
        assertThat(response.isProspect()).isTrue();
        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(response.getTimeSlotId()).isNull();
    }

    @Test
    void registerVisitor_notPublished_throws() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.DRAFT)));

        assertThatThrownBy(() -> service.registerVisitor(eventId, buildRegistrationRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void registerVisitor_duplicate_throws() {
        Visitor visitor = new Visitor();
        visitor.setId(10L);
        visitor.setFullName("Test Visitor");
        visitor.setGroupSize(2);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(visitorRepository.findById(10L)).thenReturn(Optional.of(visitor));
        when(registrationRepository.existsByEventIdAndVisitorId(eventId, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.registerVisitor(eventId, buildRegistrationRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registerVisitor_noCapacity_throws() {
        Visitor visitor = new Visitor();
        visitor.setId(10L);
        visitor.setFullName("Test Visitor");
        visitor.setGroupSize(20);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(visitorRepository.findById(10L)).thenReturn(Optional.of(visitor));
        when(registrationRepository.existsByEventIdAndVisitorId(eventId, 10L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatusNotIn(eq(eventId), any())).thenReturn(50L);

        assertThatThrownBy(() -> service.registerVisitor(eventId, buildRegistrationRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void listEvents_filtersByType() {
        Event event = buildEvent(EventStatus.PUBLISHED);
        when(eventRepository.findByType(EventType.OPEN_DAY)).thenReturn(List.of(event));
        when(registrationRepository.countByEventIdAndStatusNotIn(eq(eventId), any())).thenReturn(0L);

        List<EventResponse> responses = service.listEvents(EventType.OPEN_DAY, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getType()).isEqualTo(EventType.OPEN_DAY);
    }
}