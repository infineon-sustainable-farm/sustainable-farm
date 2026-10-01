package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FarmService {
    private final FarmRepository farmRepository;

    public FarmService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    public List<FarmResponse> findFarms() {
        return farmRepository.findAll().stream().map(FarmResponse::from).toList();
    }

    public PageResponse<FarmResponse> findFarms(Pageable pageable) {
        return toPageResponse(farmRepository.findAll(pageable).map(FarmResponse::from));
    }

    @Transactional
    public FarmResponse createFarm(FarmCreateRequest request) {
        Farm farm = new Farm();
        farm.setName(request.name());
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
        return FarmResponse.from(farmRepository.save(farm));
    }

    public FarmResponse getFarm(UUID farmId) {
        return FarmResponse.from(getFarmEntity(farmId));
    }

    @Transactional
    public FarmResponse updateFarm(UUID farmId, FarmUpdateRequest request) {
        Farm farm = getFarmEntity(farmId);
        if (request.name() != null) {
            farm.setName(request.name());
        }
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
        return FarmResponse.from(farmRepository.save(farm));
    }

    @Transactional
    public void deleteFarm(UUID farmId) {
        farmRepository.delete(getFarmEntity(farmId));
    }

    private Farm getFarmEntity(UUID farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}