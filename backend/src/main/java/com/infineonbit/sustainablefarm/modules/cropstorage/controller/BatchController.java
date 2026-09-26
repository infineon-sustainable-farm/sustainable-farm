package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public Batch create(@Valid @RequestBody Batch batch) {
        return batchService.create(batch);
    }

    @GetMapping
    public List<Batch> findAll() {
        return batchService.findAll();
    }

    @GetMapping("/fifo")
    public List<Batch> fifo() {
        return batchService.fifo();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return batchService.summary();
    }

    @GetMapping("/expiring")
    public List<Batch> expiring() {
        return batchService.expiring();
    }
}