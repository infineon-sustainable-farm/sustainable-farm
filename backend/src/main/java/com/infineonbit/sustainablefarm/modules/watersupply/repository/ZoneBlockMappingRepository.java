package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.ZoneBlockMapping;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneBlockMappingRepository extends JpaRepository<ZoneBlockMapping, UUID> {
    List<ZoneBlockMapping> findByZoneId(UUID zoneId);

    /** Mappings of a page of zones in one query (avoids one lookup per irrigation/consumption row). */
    List<ZoneBlockMapping> findByZoneIdIn(Collection<UUID> zoneIds);

    List<ZoneBlockMapping> findByBlockCode(String blockCode);

    List<ZoneBlockMapping> findByFarmIdAndBlockCode(UUID farmId, String blockCode);
}
