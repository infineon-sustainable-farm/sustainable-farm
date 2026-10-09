package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CompetitorPrice;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CompetitorPriceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompetitorPriceService {

    private final CompetitorPriceRepository repository;

    public CompetitorPriceService(CompetitorPriceRepository repository) {
        this.repository = repository;
    }

    public List<CompetitorPrice> getAll() {
        return repository.findAll();
    }

    public CompetitorPrice getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("CompetitorPrice not found with id: " + id));
    }

    public CompetitorPrice create(CompetitorPrice item) {
        return repository.save(item);
    }

    public CompetitorPrice update(Integer id, CompetitorPrice updatedData) {
        CompetitorPrice existing = getById(id);

        existing.setProductId(updatedData.getProductId());
        existing.setCompetitorName(updatedData.getCompetitorName());
        existing.setPriceEur(updatedData.getPriceEur());
        existing.setPriceDate(updatedData.getPriceDate());
        existing.setIsTarget(updatedData.getIsTarget());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}