package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SupplierResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.CropStorageMapper;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.SupplierCreateRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crop-storage/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final CropStorageMapper mapper;

    @PostMapping
    public SupplierResponse create(@Valid @RequestBody SupplierCreateRequest request) {
        return mapper.supplier(supplierService.create(request.toEntity()));
    }

    @GetMapping
    public List<SupplierResponse> findAll() {
        return CropStorageMapper.all(supplierService.findAll(), mapper::supplier);
    }
}