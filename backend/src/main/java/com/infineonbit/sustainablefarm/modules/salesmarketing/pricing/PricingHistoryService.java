package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingHistoryService {

    private final PricingHistoryRepository repository;

    public PricingHistoryService(PricingHistoryRepository repository) {
        this.repository = repository;
    }

    public List<PricingHistory> getAll() {
        return repository.findAll();
    }

    public PricingHistory getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("PricingHistory not found with id: " + id));
    }

    public PricingHistory create(PricingHistory item) {
        return repository.save(item);
    }

    public PricingHistory update(Integer id, PricingHistory updatedData) {
        PricingHistory existing = getById(id);

        existing.setProductId(updatedData.getProductId());
        existing.setPriceDate(updatedData.getPriceDate());
        existing.setPriceEurPerKg(updatedData.getPriceEurPerKg());
        existing.setHarvestSeasonFactor(updatedData.getHarvestSeasonFactor());
        existing.setSeasonLabel(updatedData.getSeasonLabel());
        existing.setStockLevelTons(updatedData.getStockLevelTons());
        existing.setStockTargetTons(updatedData.getStockTargetTons());
        existing.setDemandFactor(updatedData.getDemandFactor());
        existing.setDemandIndexLabel(updatedData.getDemandIndexLabel());
        existing.setIsApplied(updatedData.getIsApplied());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}