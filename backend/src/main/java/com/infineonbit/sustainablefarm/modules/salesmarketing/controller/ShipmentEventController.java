package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ShipmentEventService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipment-events")
public class ShipmentEventController {

    private final ShipmentEventService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ShipmentEventController(ShipmentEventService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<ShipmentEvent> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ShipmentEvent getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentEvent create(@Valid @RequestBody ShipmentEventRequest request) {
        return service.create(requestMapper.toEntity(request, ShipmentEvent.class));
    }

    @PutMapping("/{id}")
    public ShipmentEvent update(
            @PathVariable Integer id,
            @Valid @RequestBody ShipmentEventRequest request) {
        return service.update(id, requestMapper.toEntity(request, ShipmentEvent.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}