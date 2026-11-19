package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelTargetRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.SalesChannelTargetResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.SalesChannelTargetService;
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
@RequestMapping("/api/sales-marketing/sales-channel-targets")
public class SalesChannelTargetController {

    private final SalesChannelTargetService service;

    public SalesChannelTargetController(SalesChannelTargetService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesChannelTargetResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SalesChannelTargetResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesChannelTargetResponse create(@Valid @RequestBody SalesChannelTargetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SalesChannelTargetResponse update(@PathVariable Long id, @Valid @RequestBody SalesChannelTargetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
