package com.infineonbit.sustainablefarm.modules.farm.dto;

import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import java.time.Instant;

/**
 * API view of a block under its Water Supply name, a field (same JSON as before the farm module),
 * with its Plants block {@code code}, {@code null} while the field has none.
 */
public record FieldResponse(Long id, Long farmId, String name, Double areaHectares,
                            String cropType, String soilType, String coordinates, Instant createdAt,
                            String code) {
    public static FieldResponse from(Block block) {
        return new FieldResponse(block.getId(), block.getFarm().getId(), block.getName(), block.getAreaHectares(),
                block.getCropType(), block.getSoilType(), block.getCoordinates(), block.getCreatedAt(),
                block.getCode());
    }
}
