package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
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
@RequestMapping("/api/loss-records")
@RequiredArgsConstructor
public class LossRecordController {

    private final LossRecordService lossRecordService;

    @PostMapping
    public LossRecord create(@Valid @RequestBody LossRecord record) {
        return lossRecordService.create(record);
    }

    @GetMapping
    public List<LossRecord> findAll() {
        return lossRecordService.findAll();
    }

    @GetMapping("/batch/{batchId}")
    public List<LossRecord> findByBatch(@PathVariable Long batchId) {
        return lossRecordService.findByBatch(batchId);
    }
}