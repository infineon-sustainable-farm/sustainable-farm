package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.LossRecordResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.LossRecordCreateRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.LossRecordService;
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
@RequestMapping("/api/crop-storage/loss-records")
@RequiredArgsConstructor
public class LossRecordController {

    private final LossRecordService lossRecordService;
    private final CropStorageMapper mapper;

    @PostMapping
    public LossRecordResponse create(@Valid @RequestBody LossRecordCreateRequest request) {
        return mapper.lossRecord(lossRecordService.create(request.toEntity()));
    }

    @GetMapping
    public List<LossRecordResponse> findAll() {
        return CropStorageMapper.all(lossRecordService.findAll(), mapper::lossRecord);
    }

    @GetMapping("/batch/{batchId}")
    public List<LossRecordResponse> findByBatch(@PathVariable Long batchId) {
        return CropStorageMapper.all(lossRecordService.findByBatch(batchId), mapper::lossRecord);
    }
}