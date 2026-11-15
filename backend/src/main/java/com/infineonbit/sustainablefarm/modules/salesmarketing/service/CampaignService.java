package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository repository;

    public CampaignService(CampaignRepository repository) {
        this.repository = repository;
    }

    public List<Campaign> getAll() {
        return repository.findAll();
    }

    public Campaign getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found with id: " + id));
    }

    public Campaign create(Campaign item) {
        return repository.save(item);
    }

    public Campaign update(Integer id, Campaign updatedData) {
        Campaign existing = getById(id);

        existing.setName(updatedData.getName());
        existing.setStatus(updatedData.getStatus());
        existing.setBudgetEur(updatedData.getBudgetEur());
        existing.setBudgetSpentEur(updatedData.getBudgetSpentEur());
        existing.setEstimatedReach(updatedData.getEstimatedReach());
        existing.setCurrency(updatedData.getCurrency());
        existing.setStartDate(updatedData.getStartDate());
        existing.setEndDate(updatedData.getEndDate());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}