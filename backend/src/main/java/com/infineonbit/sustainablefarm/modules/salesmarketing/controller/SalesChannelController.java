package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.SalesChannelResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelService;
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
@RequestMapping("/api/sales-marketing/sales-channels")
public class SalesChannelController {

    private final SalesChannelService service;

    public SalesChannelController(SalesChannelService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesChannelResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SalesChannelResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesChannelResponse create(@Valid @RequestBody SalesChannelRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SalesChannelResponse update(@PathVariable Long id, @Valid @RequestBody SalesChannelRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
