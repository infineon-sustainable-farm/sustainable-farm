package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementCreateRequest(
        @NotBlank String type,
        @NotNull @PositiveOrZero BigDecimal quantityKg,
        @Valid EntityReferenceRequest batch,
        @Valid EntityReferenceRequest fromZone,
        @Valid EntityReferenceRequest toZone,
        String reason,
        @NotNull LocalDateTime movedAt) {

    public StockMovement toEntity() {
        StockMovement movement = new StockMovement();
        movement.setType(type);
        movement.setQuantityKg(quantityKg);
        movement.setBatch(batch == null ? null : batch.toBatch());
        movement.setFromZone(fromZone == null ? null : fromZone.toStorageZone());
        movement.setToZone(toZone == null ? null : toZone.toStorageZone());
        movement.setReason(reason);
        movement.setMovedAt(movedAt);
        return movement;
    }
}
