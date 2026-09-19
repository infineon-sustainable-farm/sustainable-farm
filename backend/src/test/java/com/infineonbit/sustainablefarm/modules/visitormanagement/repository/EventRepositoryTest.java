package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private EventRepository repository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    private LocalDateTime start = LocalDateTime.of(2026, 10, 4, 10, 0);

    @BeforeEach
    void setUp() {
    }

    private Event createEvent(EventType type, EventStatus status) {
        Event event = new Event();
        event.setTitle("Test Event");
        event.setType(type);
        event.setStartDateTime(start);
        event.setEndDateTime(start.plusHours(6));
        event.setMaxCapacity(50);
        event.setStatus(status);
        return repository.save(event);
    }

    @Test
    void findByType_returnsMatchingEvents() {
        createEvent(EventType.OPEN_DAY, EventStatus.DRAFT);
        createEvent(EventType.SCHOOL, EventStatus.PUBLISHED);

        List<Event> result = repository.findByType(EventType.OPEN_DAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(EventType.OPEN_DAY);
    }

    @Test
    void findByStatus_returnsMatchingEvents() {
        createEvent(EventType.OPEN_DAY, EventStatus.DRAFT);
        createEvent(EventType.SCHOOL, EventStatus.PUBLISHED);

        List<Event> result = repository.findByStatus(EventStatus.PUBLISHED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void findByStartDateTimeBetween_filtersByDate() {
        createEvent(EventType.OPEN_DAY, EventStatus.DRAFT);
        Event other = new Event();
        other.setTitle("Other Event");
        other.setType(EventType.SCHOOL);
        other.setStartDateTime(start.plusDays(5));
        other.setEndDateTime(start.plusDays(5).plusHours(3));
        other.setMaxCapacity(40);
        other.setStatus(EventStatus.DRAFT);
        repository.save(other);

        List<Event> result = repository.findByStartDateTimeBetween(
                start.minusHours(1), start.plusHours(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Event");
    }

    @Test
    void findByEventId_andCountByEventIdAndStatusNotIn() {
        Event event = createEvent(EventType.OPEN_DAY, EventStatus.PUBLISHED);
        Visitor visitor = new Visitor();
        visitor.setFullName("Marie Curie");
        visitor.setGroupSize(4);
        visitor = visitorRepository.save(visitor);

        Registration active = new Registration();
        active.setVisitor(visitor);
        active.setEventId(event.getId());
        active.setStatus(RegistrationStatus.PENDING);
        active.setVisitPurpose(VisitPurpose.PARTNERSHIP);
        registrationRepository.save(active);

        Registration rejected = new Registration();
        rejected.setVisitor(visitor);
        rejected.setEventId(event.getId());
        rejected.setStatus(RegistrationStatus.REJECTED);
        rejected.setVisitPurpose(VisitPurpose.PARTNERSHIP);
        registrationRepository.save(rejected);

        assertThat(registrationRepository.findByEventId(event.getId())).hasSize(2);
        assertThat(registrationRepository.countByEventIdAndStatusNotIn(
                event.getId(), List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED)))
                .isEqualTo(1);
        assertThat(registrationRepository.existsByEventIdAndVisitorId(event.getId(), visitor.getId()))
                .isTrue();
    }
}