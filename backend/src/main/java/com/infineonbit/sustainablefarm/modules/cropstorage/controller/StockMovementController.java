package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.MovementResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StockMovementCreateRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StockMovementService;
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
@RequestMapping("/api/crop-storage/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final CropStorageMapper mapper;

    @PostMapping
    public MovementResponse create(@Valid @RequestBody StockMovementCreateRequest request) {
        return mapper.movement(stockMovementService.create(request.toEntity()));
    }

    @GetMapping
    public List<MovementResponse> findAll() {
        return CropStorageMapper.all(stockMovementService.findAll(), mapper::movement);
    }

    @GetMapping("/batch/{batchId}")
    public List<MovementResponse> findByBatch(@PathVariable Long batchId) {
        return CropStorageMapper.all(stockMovementService.findByBatch(batchId), mapper::movement);
    }
}