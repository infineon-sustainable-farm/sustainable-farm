package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ValuationSummary;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.BatchResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageBatchCreateRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints of the batch inventory: creation, full listing, the FIFO
 * rotation queue, the multi-unit valuation summary and the expiry list.
 */
@RestController("cropStorageBatchController")
@RequestMapping("/api/crop-storage/batches")
@RequiredArgsConstructor
public class CropStorageBatchController {

    private final CropStorageBatchService batchService;
    private final CropStorageMapper mapper;

    /**
     * Creates one batch; see CropStorageBatchService for the batch code convention.
     *
     * @param request validated batch fields from the request body
     * @return the saved batch
     */
    @PostMapping
    public BatchResponse create(@Valid @RequestBody CropStorageBatchCreateRequest request) {
        return mapper.batch(batchService.create(request.toEntity(), request.mangoVariety()));
    }

    /**
     * Returns every batch for the inventory table of the dashboard.
     *
     * @return all batches
     */
    @GetMapping
    public List<BatchResponse> findAll() {
        return mapper.batches(batchService.findAll());
    }

    /**
     * Returns the rotation queue: which batch should leave the store next.
     *
     * @return non-empty batches, priority then entry date
     */
    @GetMapping("/fifo")
    public List<BatchResponse> fifo() {
        return mapper.batches(batchService.fifo());
    }

    /**
     * Returns the overview KPIs in all reporting units (kg, tonne, lb, m3,
     * FCFA, EUR, USD) for the dashboard header.
     *
     * @return the valuation in the seven reporting units
     */
    @GetMapping("/summary")
    public ValuationSummary summary() {
        return batchService.summary();
    }

    /**
     * Returns the batches that expire soonest, for the expiry panel and for
     * the future exchange with Sales and Marketing.
     *
     * @return batches with an expiry date, soonest first
     */
    @GetMapping("/expiring")
    public List<BatchResponse> expiring() {
        return mapper.batches(batchService.expiring());
    }
}