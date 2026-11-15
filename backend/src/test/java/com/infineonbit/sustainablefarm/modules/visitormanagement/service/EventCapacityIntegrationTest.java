package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the event capacity rule the same way the slot one is guarded: seats
 * are people, so a group of ten cannot pass as a single registration.
 */
@SpringBootTest
@ActiveProfiles("test")
class EventCapacityIntegrationTest {

    private static final List<RegistrationStatus> INACTIVE =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    @Autowired
    private EventService eventService;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private VisitorRepository visitorRepository;
    @Autowired
    private RegistrationRepository registrationRepository;

    private final List<Long> registrationIds = new ArrayList<>();
    private final List<Long> visitorIds = new ArrayList<>();
    private Long eventId;

    @AfterEach
    void removeCreatedRows() {
        registrationIds.forEach(registrationRepository::deleteById);
        visitorIds.forEach(visitorRepository::deleteById);
        if (eventId != null) {
            eventRepository.deleteById(eventId);
        }
    }

    private VisitorResponse createVisitor(String name, int groupSize) {
        VisitorRequest request = new VisitorRequest();
        request.setFullName(name);
        request.setGroupSize(groupSize);
        request.setType(groupSize > 1 ? VisitorType.GROUP : VisitorType.INDIVIDUAL);
        VisitorResponse created = registrationService.createVisitor(request);
        visitorIds.add(created.getId());
        return created;
    }

    private void register(Long visitorId) {
        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setVisitorId(visitorId);
        request.setVisitPurpose(VisitPurpose.TOURISM);
        RegistrationResponse created = eventService.registerVisitor(eventId, request);
        registrationIds.add(created.getId());
    }

    @Test
    void eventCapacity_countsPeopleNotRegistrations() {
        EventRequest eventRequest = new EventRequest();
        eventRequest.setTitle("Capacity event");
        eventRequest.setType(EventType.OPEN_DAY);
        eventRequest.setStartDateTime(LocalDateTime.now().plusDays(30).withHour(9).withMinute(0));
        eventRequest.setEndDateTime(LocalDateTime.now().plusDays(30).withHour(15).withMinute(0));
        eventRequest.setMaxCapacity(10);
        EventResponse event = eventService.createEvent(eventRequest);
        eventId = event.getId();
        eventService.publishEvent(eventId);

        register(createVisitor("Event group A", 4).getId());
        register(createVisitor("Event group B", 5).getId());

        long booked = registrationRepository.sumGroupSizeByEventIdAndStatusNotIn(eventId, INACTIVE);
        assertThat(booked).isEqualTo(9);

        VisitorResponse tooMany = createVisitor("Event group C", 2);
        assertThatThrownBy(() -> register(tooMany.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");
    }
}