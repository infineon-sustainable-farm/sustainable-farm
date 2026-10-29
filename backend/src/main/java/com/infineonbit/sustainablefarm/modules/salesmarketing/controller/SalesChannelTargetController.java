package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannelTarget;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelTargetService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelTargetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-channel-targets")
public class SalesChannelTargetController {

    private final SalesChannelTargetService service;
    private final SalesMarketingRequestMapper requestMapper;

    public SalesChannelTargetController(SalesChannelTargetService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
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
    public SalesChannelTarget create(@Valid @RequestBody SalesChannelTargetRequest request) {
        return service.create(requestMapper.toEntity(request, SalesChannelTarget.class));
    }

    @PutMapping("/{id}")
    public SalesChannelTarget update(
            @PathVariable Integer id,
            @Valid @RequestBody SalesChannelTargetRequest request) {
        return service.update(id, requestMapper.toEntity(request, SalesChannelTarget.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}