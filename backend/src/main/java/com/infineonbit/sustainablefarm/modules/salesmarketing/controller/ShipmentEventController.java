package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentEventRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ShipmentEventResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ShipmentEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipment-events")
public class ShipmentEventController {

    private final ShipmentEventService service;

    public ShipmentEventController(ShipmentEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShipmentEventResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ShipmentEventResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentEventResponse create(@Valid @RequestBody ShipmentEventRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ShipmentEventResponse update(@PathVariable Long id, @Valid @RequestBody ShipmentEventRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
