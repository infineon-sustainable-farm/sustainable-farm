package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageZoneService {

    private final StorageZoneRepository storageZoneRepository;

    public StorageZone create(StorageZone zone) {
        return storageZoneRepository.save(zone);
    }

    public List<StorageZone> findAll() {
        return storageZoneRepository.findAll();
    }
}