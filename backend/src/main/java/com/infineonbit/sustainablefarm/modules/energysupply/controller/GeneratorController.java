package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.GeneratorEventRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.GeneratorEventResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.service.GeneratorEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.net.URI;
import java.util.List;

/** Feeds the "Generator Backup Management" tab of the Energy App. */
@RestController("energyGeneratorController")
@RequestMapping("/api/energy/generator-events")
public class GeneratorController {

    private final GeneratorEventService service;

    public GeneratorController(GeneratorEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<GeneratorEventResponseDto> getAllEvents() {
        return service.findAll();
    }

    @GetMapping("/{eventId}")
    public GeneratorEventResponseDto getEvent(@PathVariable String eventId) {
        return service.findById(eventId);
    }

    @PostMapping
    public ResponseEntity<GeneratorEventResponseDto> createEvent(
            @Valid @RequestBody GeneratorEventRequest request) {
        GeneratorEventResponseDto response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{eventId}")
                .buildAndExpand(response.eventId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{eventId}")
    public GeneratorEventResponseDto updateEvent(
            @PathVariable String eventId,
            @Valid @RequestBody GeneratorEventRequest request) {
        return service.update(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String eventId) {
        service.delete(eventId);
        return ResponseEntity.noContent().build();
    }
}