package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ReportFile;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ReportFileService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ReportFileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report-files")
public class ReportFileController {

    private final ReportFileService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ReportFileController(ReportFileService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<ReportFile> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ReportFile getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportFile create(@Valid @RequestBody ReportFileRequest request) {
        return service.create(requestMapper.toEntity(request, ReportFile.class));
    }

    @PutMapping("/{id}")
    public ReportFile update(
            @PathVariable Integer id,
            @Valid @RequestBody ReportFileRequest request) {
        return service.update(id, requestMapper.toEntity(request, ReportFile.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
