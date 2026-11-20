package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.entity.Zone;

/** Entity to {@code farm.api} views; called inside a transaction (the block of a zone is lazy). */
final class FarmMapper {
    private FarmMapper() {
    }

    static FarmDto toDto(Farm farm) {
        return new FarmDto(farm.getId(), farm.getName(), farm.getDescription(), farm.getAddress(),
                farm.getLatitude(), farm.getLongitude(), farm.getAreaHectares(), farm.getCreatedAt());
    }

    static BlockDto toDto(Block block) {
        return new BlockDto(block.getId(), block.getFarm().getId(), block.getCode(), block.getName(),
                block.getAreaHectares(), block.getCropType(), block.getSoilType(), block.getCoordinates(),
                block.getCreatedAt());
    }

    static ZoneDto toDto(Zone zone) {
        Block block = zone.getBlock();
        return new ZoneDto(zone.getId(), block.getId(), block.getFarm().getId(), block.getCode(), zone.getName(),
                zone.getAreaHectares(), zone.getIrrigationMethod(), zone.getCropCoefficient(),
                zone.getEmitterCount(), zone.getEmitterNominalFlowLh(), zone.getCreatedAt());
    }
}
