package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandReportRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.DemandReportResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.DemandReportService;
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
@RequestMapping("/api/sales-marketing/demand-reports")
public class DemandReportController {

    private final DemandReportService service;

    public DemandReportController(DemandReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<DemandReportResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DemandReportResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandReportResponse create(@Valid @RequestBody DemandReportRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DemandReportResponse update(@PathVariable Long id, @Valid @RequestBody DemandReportRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
