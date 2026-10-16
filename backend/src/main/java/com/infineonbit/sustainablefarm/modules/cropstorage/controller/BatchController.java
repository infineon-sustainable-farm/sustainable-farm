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

/**
 * REST endpoints of the batch inventory: creation, full listing, the FIFO
 * rotation queue, the multi-unit valuation summary and the expiry list.
 */
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    /**
     * Creates one batch; see BatchService for the batch code convention.
     *
     * @param batch batch fields from the request body
     * @return the saved batch
     */
    @PostMapping
    public Batch create(@Valid @RequestBody Batch batch) {
        return batchService.create(batch);
    }

    /**
     * Returns every batch for the inventory table of the dashboard.
     *
     * @return all batches
     */
    @GetMapping
    public List<Batch> findAll() {
        return batchService.findAll();
    }

    /**
     * Returns the rotation queue: which batch should leave the store next.
     *
     * @return non-empty batches, priority then entry date
     */
    @GetMapping("/fifo")
    public List<Batch> fifo() {
        return batchService.fifo();
    }

    /**
     * Returns the overview KPIs in all reporting units (kg, tonne, lb, m3,
     * FCFA, EUR, USD) for the dashboard header.
     *
     * @return the seven-entry valuation map
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return batchService.summary();
    }

    /**
     * Returns the batches that expire soonest, for the expiry panel and for
     * the future exchange with Sales and Marketing.
     *
     * @return batches with an expiry date, soonest first
     */
    @GetMapping("/expiring")
    public List<Batch> expiring() {
        return batchService.expiring();
    }
}