package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageZoneCreateRequest;
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
@RequestMapping("/api/crop-storage/storage-zones")
@RequiredArgsConstructor
public class StorageZoneController {

    private final StorageZoneService storageZoneService;
    private final CropStorageMapper mapper;

    @PostMapping
    public ZoneResponse create(@Valid @RequestBody StorageZoneCreateRequest request) {
        return mapper.zone(storageZoneService.create(request.toEntity()));
    }

    @GetMapping
    public List<ZoneResponse> findAll() {
        return CropStorageMapper.all(storageZoneService.findAll(), mapper::zone);
    }
}