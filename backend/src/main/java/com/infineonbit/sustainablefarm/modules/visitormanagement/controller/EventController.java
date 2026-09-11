package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) EventType type,
                                    @RequestParam(required = false) LocalDate date) {
        return eventService.listEvents(type, date);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        eventService.cancelEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id) {
        return eventService.publishEvent(id);
    }

    @GetMapping("/{id}/registrations")
    public List<RegistrationResponse> registrations(@PathVariable Long id) {
        return eventService.getRegistrations(id);
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long id,
                                                         @Valid @RequestBody EventRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.registerVisitor(id, request));
    }
}