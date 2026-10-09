package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannelTarget;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelTargetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-channel-targets")
@CrossOrigin(origins = "http://localhost:3000")
public class SalesChannelTargetController {

    private final SalesChannelTargetService service;

    public SalesChannelTargetController(SalesChannelTargetService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesChannelTarget> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SalesChannelTarget getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesChannelTarget create(@Valid @RequestBody SalesChannelTarget item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public SalesChannelTarget update(
            @PathVariable Integer id,
            @Valid @RequestBody SalesChannelTarget item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}