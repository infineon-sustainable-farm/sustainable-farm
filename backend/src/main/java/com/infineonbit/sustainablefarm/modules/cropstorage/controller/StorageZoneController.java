package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StorageZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storage-zones")
@RequiredArgsConstructor
public class StorageZoneController {

    private final StorageZoneService storageZoneService;

    @PostMapping
    public StorageZone create(@Valid @RequestBody StorageZone zone) {
        return storageZoneService.create(zone);
    }

    @GetMapping
    public List<StorageZone> findAll() {
        return storageZoneService.findAll();
    }
}