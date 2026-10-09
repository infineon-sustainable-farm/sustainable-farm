package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-channels")
@CrossOrigin(origins = "http://localhost:3000")
public class SalesChannelController {

    private final SalesChannelService service;

    public SalesChannelController(SalesChannelService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesChannel> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SalesChannel getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesChannel create(@Valid @RequestBody SalesChannel item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public SalesChannel update(@PathVariable Integer id, @Valid @RequestBody SalesChannel item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}