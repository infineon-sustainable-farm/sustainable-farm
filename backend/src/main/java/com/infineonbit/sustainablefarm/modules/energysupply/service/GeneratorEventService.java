package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.GeneratorEventRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.GeneratorEventResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.GeneratorEventNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service("energyGeneratorEventService")
@Transactional(readOnly = true)
public class GeneratorEventService {

    private final GeneratorEventRepository energyGeneratorEventRepository;

    public GeneratorEventService(GeneratorEventRepository energyGeneratorEventRepository) {
        this.energyGeneratorEventRepository = energyGeneratorEventRepository;
    }

    public List<GeneratorEventResponseDto> findAll() {
        return energyGeneratorEventRepository.findAllByOrderByDateDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GeneratorEventResponseDto findById(String eventId) {
        return toResponse(findEntity(eventId));
    }

    @Transactional
    public GeneratorEventResponseDto create(GeneratorEventRequest request) {
        GeneratorEvent event = new GeneratorEvent();
        applyRequest(event, request);
        return toResponse(energyGeneratorEventRepository.save(event));
    }

    @Transactional
    public GeneratorEventResponseDto update(String eventId, GeneratorEventRequest request) {
        GeneratorEvent event = findEntity(eventId);
        applyRequest(event, request);
        return toResponse(energyGeneratorEventRepository.save(event));
    }

    @Transactional
    public void delete(String eventId) {
        energyGeneratorEventRepository.delete(findEntity(eventId));
    }

    private GeneratorEvent findEntity(String eventId) {
        return energyGeneratorEventRepository.findById(eventId)
                .orElseThrow(() -> new GeneratorEventNotFoundException(eventId));
    }

    private void applyRequest(GeneratorEvent event, GeneratorEventRequest request) {
        event.setDate(request.date());
        event.setTriggerReason(request.triggerReason());
        event.setDurationHours(request.durationHours());
        event.setFuelLiters(request.fuelLiters());
        event.setCostEur(request.costEur());
    }

    private GeneratorEventResponseDto toResponse(GeneratorEvent event) {
        return new GeneratorEventResponseDto(
                event.getEventId(),
                event.getDate(),
                event.getTriggerReason(),
                event.getDurationHours(),
                event.getFuelLiters(),
                event.getCostEur());
    }
}
