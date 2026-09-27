package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ShipmentEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipment-events")
@CrossOrigin(origins = "http://localhost:3000")
public class ShipmentEventController {

    private final ShipmentEventService service;

    public ShipmentEventController(ShipmentEventService service) {
        this.service = service;
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
    public ShipmentEvent create(@Valid @RequestBody ShipmentEvent item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public ShipmentEvent update(
            @PathVariable Integer id,
            @Valid @RequestBody ShipmentEvent item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}