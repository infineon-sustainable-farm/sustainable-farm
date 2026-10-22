package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-channels")
public class SalesChannelController {

    private final SalesChannelService service;
    private final SalesMarketingRequestMapper requestMapper;

    public SalesChannelController(SalesChannelService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
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
    public SalesChannel create(@Valid @RequestBody SalesChannelRequest request) {
        return service.create(requestMapper.toEntity(request, SalesChannel.class));
    }

    @PutMapping("/{id}")
    public SalesChannel update(@PathVariable Integer id, @Valid @RequestBody SalesChannelRequest request) {
        return service.update(id, requestMapper.toEntity(request, SalesChannel.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}