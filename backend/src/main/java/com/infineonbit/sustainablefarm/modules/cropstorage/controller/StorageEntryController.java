package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StorageEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-005 endpoints consumed by Product Transformation: storage entries of
 * dried mango once packaging is complete.
 */
@RestController
@RequestMapping("/api/crop-storage/entries")
@RequiredArgsConstructor
public class StorageEntryController {

    private final StorageEntryService storageEntryService;

    /**
     * Registers one storage entry: creates the batch and its IN movement.
     *
     * @param request the snake_case API-005 payload
     * @return 201 with the entry id, zone, timestamp, remaining capacity and
     *         status; 404 for an unknown zone, 409 for a capacity overflow
     *         or an already registered batch_id
     */
    @PostMapping
    public ResponseEntity<StorageEntryResponse> create(@Valid @RequestBody StorageEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageEntryService.registerEntry(request));
    }
}