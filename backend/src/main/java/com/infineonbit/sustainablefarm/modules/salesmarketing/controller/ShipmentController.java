package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ShipmentService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ShipmentController(ShipmentService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<Shipment> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Shipment getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment create(@Valid @RequestBody ShipmentRequest request) {
        return service.create(requestMapper.toEntity(request, Shipment.class));
    }

    @PutMapping("/{id}")
    public Shipment update(
            @PathVariable Integer id,
            @Valid @RequestBody ShipmentRequest request) {
        return service.update(id, requestMapper.toEntity(request, Shipment.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}