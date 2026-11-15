package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
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
import java.util.Map;

/**
 * REST endpoints of the batch inventory: creation, full listing, the FIFO
 * rotation queue, the multi-unit valuation summary and the expiry list.
 */
@RestController("cropStorageBatchController")
@RequestMapping("/api/crop-storage/batches")
@RequiredArgsConstructor
public class CropStorageBatchController {

    private final CropStorageBatchService batchService;

    /**
     * Creates one batch; see CropStorageBatchService for the batch code convention.
     *
     * @param request validated batch fields from the request body
     * @return the saved batch
     */
    @PostMapping
    public CropStorageBatch create(@Valid @RequestBody CropStorageBatchCreateRequest request) {
        return batchService.create(request.toEntity());
    }

    /**
     * Returns every batch for the inventory table of the dashboard.
     *
     * @return all batches
     */
    @GetMapping
    public List<CropStorageBatch> findAll() {
        return batchService.findAll();
    }

    /**
     * Returns the rotation queue: which batch should leave the store next.
     *
     * @return non-empty batches, priority then entry date
     */
    @GetMapping("/fifo")
    public List<CropStorageBatch> fifo() {
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
    public List<CropStorageBatch> expiring() {
        return batchService.expiring();
    }
}