package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import jakarta.validation.constraints.NotBlank;

public record SupplierCreateRequest(
        @NotBlank String supplierCode,
        @NotBlank String name,
        String region,
        String contactPhone) {

    public Supplier toEntity() {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(supplierCode);
        supplier.setName(name);
        supplier.setRegion(region);
        supplier.setContactPhone(contactPhone);
        return supplier;
    }
}
