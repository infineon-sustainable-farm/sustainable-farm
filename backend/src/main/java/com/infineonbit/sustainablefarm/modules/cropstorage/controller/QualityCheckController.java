package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.QualityCheckResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.QualityCheckCreateRequest;
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
@RequestMapping("/api/crop-storage/quality-checks")
@RequiredArgsConstructor
public class QualityCheckController {

    private final QualityCheckService qualityCheckService;
    private final CropStorageMapper mapper;

    @PostMapping
    public QualityCheckResponse create(@Valid @RequestBody QualityCheckCreateRequest request) {
        return mapper.qualityCheck(qualityCheckService.create(request.toEntity()));
    }

    @GetMapping
    public List<QualityCheckResponse> findAll() {
        return mapper.qualityChecks(qualityCheckService.findAll());
    }

    @GetMapping("/batch/{batchId}")
    public List<QualityCheckResponse> findByBatch(@PathVariable Long batchId) {
        return mapper.qualityChecks(qualityCheckService.findByBatch(batchId));
    }
}