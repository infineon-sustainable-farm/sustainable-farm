package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ReportFileRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ReportFileResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ReportFileService;
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
@RequestMapping("/api/sales-marketing/report-files")
public class ReportFileController {

    private final ReportFileService service;

    public ReportFileController(ReportFileService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportFileResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ReportFileResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportFileResponse create(@Valid @RequestBody ReportFileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ReportFileResponse update(@PathVariable Long id, @Valid @RequestBody ReportFileRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
