package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignMetricRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignMetricService {

    private final CampaignMetricRepository repository;

    public CampaignMetricService(CampaignMetricRepository repository) {
        this.repository = repository;
    }

    public List<CampaignMetric> getAll() {
        return repository.findAll();
    }

    public CampaignMetric getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "CampaignMetric not found with id: " + id));
    }

    public CampaignMetric create(CampaignMetric item) {
        return repository.save(item);
    }

    public CampaignMetric update(
            Integer id,
            CampaignMetric updatedData) {

        CampaignMetric existing = getById(id);

        existing.setCampaignId(updatedData.getCampaignId());
        existing.setMetricDate(updatedData.getMetricDate());
        existing.setClicks(updatedData.getClicks());
        existing.setSignups(updatedData.getSignups());
        existing.setReach(updatedData.getReach());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}