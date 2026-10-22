package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "zone_block_mapping", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zone_block_mapping_zone_block", columnNames = {"zone_id", "block_code"})
})
public class ZoneBlockMapping extends BaseEntity {
    @NotNull
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @NotBlank
    @Column(name = "block_code", nullable = false, length = 50)
    private String blockCode;

    @NotNull
    @Column(name = "farm_id", nullable = false)
    private UUID farmId;

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }
}
