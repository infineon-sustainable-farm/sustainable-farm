package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReport;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.DemandReportService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demand-reports")
public class DemandReportController {

    private final DemandReportService service;
    private final SalesMarketingRequestMapper requestMapper;

    public DemandReportController(DemandReportService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<DemandReport> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DemandReport getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandReport create(@Valid @RequestBody DemandReportRequest request) {
        return service.create(requestMapper.toEntity(request, DemandReport.class));
    }

    @PutMapping("/{id}")
    public DemandReport update(
            @PathVariable Integer id,
            @Valid @RequestBody DemandReportRequest request) {
        return service.update(id, requestMapper.toEntity(request, DemandReport.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}