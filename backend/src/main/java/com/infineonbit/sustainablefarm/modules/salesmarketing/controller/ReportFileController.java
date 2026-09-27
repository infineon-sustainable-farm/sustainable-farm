package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ReportFile;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ReportFileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report-files")
@CrossOrigin(origins = "http://localhost:3000")
public class ReportFileController {

    private final ReportFileService service;

    public ReportFileController(ReportFileService service) {
        this.service = service;
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
    public ReportFile create(@Valid @RequestBody ReportFile item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public ReportFile update(
            @PathVariable Integer id,
            @Valid @RequestBody ReportFile item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
