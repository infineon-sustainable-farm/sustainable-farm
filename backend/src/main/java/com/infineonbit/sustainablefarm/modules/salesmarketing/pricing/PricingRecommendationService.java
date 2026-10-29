package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingRecommendation;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingRecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingRecommendationService {

    private final PricingRecommendationRepository repository;

    public PricingRecommendationService(PricingRecommendationRepository repository) {
        this.repository = repository;
    }

    public List<PricingRecommendation> getAll() {
        return repository.findAll();
    }

    public PricingRecommendation getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PricingRecommendation not found with id: " + id));
    }

    public PricingRecommendation create(PricingRecommendation item) {
        return repository.save(item);
    }

    public PricingRecommendation update(
            Integer id,
            PricingRecommendation updatedData) {

        PricingRecommendation existing = getById(id);

        existing.setProductId(updatedData.getProductId());
        existing.setMessage(updatedData.getMessage());
        existing.setSuggestedPriceEur(updatedData.getSuggestedPriceEur());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}