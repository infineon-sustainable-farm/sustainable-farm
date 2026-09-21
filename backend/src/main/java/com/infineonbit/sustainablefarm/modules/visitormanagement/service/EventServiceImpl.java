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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private static final List<RegistrationStatus> INACTIVE_STATUSES =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    private static final List<RegistrationStatus> ACTIVE_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED,
                    RegistrationStatus.CHECKED_IN);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final VisitorRepository visitorRepository;

    public EventServiceImpl(EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            VisitorRepository visitorRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.visitorRepository = visitorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(EventType type, LocalDate date) {
        List<Event> events;
        if (date != null) {
            events = eventRepository.findByStartDateTimeBetween(
                    date.atStartOfDay(), date.atTime(LocalTime.MAX));
        } else if (type != null) {
            events = eventRepository.findByType(type);
        } else {
            events = eventRepository.findAll();
        }
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : registrationRepository.countByEventIds(
                events.stream().map(Event::getId).collect(Collectors.toList()),
                INACTIVE_STATUSES)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return events.stream()
                .map(event -> EventResponse.from(event, counts.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(int page, int size) {
        Page<Event> events = eventRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDateTime")));
        if (events.isEmpty()) {
            return Page.empty(events.getPageable());
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : registrationRepository.countByEventIds(
                events.getContent().stream().map(Event::getId).collect(Collectors.toList()),
                INACTIVE_STATUSES)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return events.map(event -> EventResponse.from(event, counts.getOrDefault(event.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        return toResponse(getEventEntity(id));
    }

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        validateCommon(request);
        Event event = new Event();
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = getEventEntity(id);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled event " + id);
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot update a completed event " + id);
        }
        validateCommon(request);
        apply(event, request);
        eventRepository.save(event);
        eventRepository.flush();
        return toResponse(event);
    }

    @Override
    @Transactional
    public void cancelEvent(Long id) {
        Event event = getEventEntityForUpdate(id);
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed event " + id);
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            return;
        }
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        List<Registration> registrations = registrationRepository
                .findByEventIdAndStatusIn(id, ACTIVE_STATUSES);
        for (Registration registration : registrations) {
            registration.setStatus(RegistrationStatus.CANCELLED);
        }
        registrationRepository.saveAll(registrations);
    }

    @Override
    @Transactional
    public EventResponse publishEvent(Long id) {
        Event event = getEventEntity(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published (current: "
                    + event.getStatus() + ")");
        }
        if (event.getEndDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish an event that has already ended");
        }
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        eventRepository.flush();
        return toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrations(Long eventId) {
        getEventEntity(eventId);
        return registrationRepository.findByEventId(eventId).stream()
                .map(RegistrationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegistrationResponse registerVisitor(Long eventId, EventRegistrationRequest request) {
        Event event = getEventEntityForUpdate(eventId);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Cannot register on an event that is not PUBLISHED "
                    + "(current: " + event.getStatus() + ")");
        }
        Visitor visitor = request.getVisitorId() != null
                ? getVisitorEntity(request.getVisitorId())
                : null;
        if (visitor != null
                && registrationRepository.existsByEventIdAndVisitorId(eventId, visitor.getId())) {
            throw new ConflictException("Visitor " + visitor.getId()
                    + " is already registered on event " + eventId);
        }
        int groupSize = visitor != null ? visitor.getGroupSize() : 1;
        long booked = registrationRepository.countByEventIdAndStatusNotIn(eventId, INACTIVE_STATUSES);
        if (booked + groupSize > event.getMaxCapacity()) {
            throw new BusinessRuleException("Not enough capacity on event " + eventId
                    + " (max " + event.getMaxCapacity() + ")");
        }

        Registration registration = new Registration();
        registration.setVisitor(visitor);
        registration.setEventId(eventId);
        registration.setVisitPurpose(request.getVisitPurpose());
        registration.setProspect(isCommercialPurpose(request.getVisitPurpose()));
        registration.setStatus(RegistrationStatus.PENDING);
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    private void validateCommon(EventRequest request) {
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new BusinessRuleException("endDateTime must be after startDateTime");
        }
    }

    private void apply(Event event, EventRequest request) {
        event.setTitle(request.getTitle());
        event.setType(request.getType());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setMaxCapacity(request.getMaxCapacity() == null ? 10 : request.getMaxCapacity());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
    }

    private Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
    }

    private Event getEventEntityForUpdate(Long id) {
        return eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
    }

    private Visitor getVisitorEntity(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor " + id + " not found"));
    }

    private long countBooked(Long eventId) {
        return registrationRepository.countByEventIdAndStatusNotIn(eventId, INACTIVE_STATUSES);
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.from(event, countBooked(event.getId()));
    }

    private boolean isCommercialPurpose(VisitPurpose purpose) {
        return purpose == VisitPurpose.PURCHASE
                || purpose == VisitPurpose.PARTNERSHIP
                || purpose == VisitPurpose.INVESTMENT;
    }
}