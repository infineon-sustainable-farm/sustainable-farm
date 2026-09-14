package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for events (open days, buyer visits, school days,
 * community celebrations). Events carry their own capacity and accept
 * registrations that are not tied to a guided-tour time slot.
 */
public interface EventService {

    List<EventResponse> listEvents(EventType type, LocalDate date);

    EventResponse getEvent(Long id);

    EventResponse createEvent(EventRequest request);

    EventResponse updateEvent(Long id, EventRequest request);

    void cancelEvent(Long id);

    EventResponse publishEvent(Long id);

    List<RegistrationResponse> getRegistrations(Long eventId);

    RegistrationResponse registerVisitor(Long eventId, EventRegistrationRequest request);
}