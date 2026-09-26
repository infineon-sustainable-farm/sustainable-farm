package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
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
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    public StockMovement create(@Valid @RequestBody StockMovement movement) {
        return stockMovementService.create(movement);
    }

    @GetMapping
    public List<StockMovement> findAll() {
        return stockMovementService.findAll();
    }

    @GetMapping("/batch/{batchId}")
    public List<StockMovement> findByBatch(@PathVariable Long batchId) {
        return stockMovementService.findByBatch(batchId);
    }
}