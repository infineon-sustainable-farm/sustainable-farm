package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.QualityCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quality-checks")
@RequiredArgsConstructor
public class QualityCheckController {

    private final QualityCheckService qualityCheckService;

    @PostMapping
    public QualityCheck create(@Valid @RequestBody QualityCheck check) {
        return qualityCheckService.create(check);
    }

    @GetMapping
    public List<QualityCheck> findAll() {
        return qualityCheckService.findAll();
    }

    @GetMapping("/batch/{batchId}")
    public List<QualityCheck> findByBatch(@PathVariable Long batchId) {
        return qualityCheckService.findByBatch(batchId);
    }
}