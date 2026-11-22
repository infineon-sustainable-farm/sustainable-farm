package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StorageEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API-005 endpoints consumed by Product Transformation: storage entries of
 * dried mango once packaging is complete. Contract still a draft to align
 * between both teams.
 */
@RestController("cropStorageEntryController")
@RequestMapping("/api/crop-storage/entries")
@RequiredArgsConstructor
public class StorageEntryController {

    private final StorageEntryService storageEntryService;

    /**
     * Registers one storage entry: creates the batch and its IN movement.
     *
     * @param request the snake_case API-005 payload
     * @return 201 with the entry id, zone, UTC timestamp, remaining capacity and
     *         status; 400 for an invalid payload, 404 for an unknown zone, 409 for
     *         a capacity overflow or an already registered batch_id
     */
    @PostMapping
    public ResponseEntity<StorageEntryResponse> create(@Valid @RequestBody StorageEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageEntryService.registerEntry(request));
    }

    /**
     * @param storageEntryId the storage_entry_id returned on creation
     * @return the current state of the entry; 404 when unknown, 400 when not numeric
     */
    @GetMapping("/{storageEntryId}")
    public StorageEntryResponse findOne(@PathVariable Long storageEntryId) {
        return storageEntryService.findEntry(storageEntryId);
    }

    /**
     * @param batchId Product Transformation batch_id
     * @return the entries of that batch (empty list when none)
     */
    @GetMapping("/batch/{batchId}")
    public List<StorageEntryResponse> findByBatch(@PathVariable String batchId) {
        return storageEntryService.findEntriesByBatchId(batchId);
    }
}
