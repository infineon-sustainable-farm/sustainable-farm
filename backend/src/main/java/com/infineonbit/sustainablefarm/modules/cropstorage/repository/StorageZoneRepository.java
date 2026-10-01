package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageZoneRepository extends JpaRepository<StorageZone, Long> {
}