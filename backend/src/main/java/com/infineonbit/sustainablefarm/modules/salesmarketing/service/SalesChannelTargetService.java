package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannelTarget;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelTargetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesChannelTargetService {

    private final SalesChannelTargetRepository repository;

    public SalesChannelTargetService(SalesChannelTargetRepository repository) {
        this.repository = repository;
    }

    public List<SalesChannelTarget> getAll() {
        return repository.findAll();
    }

    public SalesChannelTarget getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "SalesChannelTarget not found with id: " + id));
    }

    public SalesChannelTarget create(SalesChannelTarget item) {
        return repository.save(item);
    }

    public SalesChannelTarget update(Integer id, SalesChannelTarget updatedData) {
        SalesChannelTarget existing = getById(id);

        existing.setChannelId(updatedData.getChannelId());
        existing.setPeriodStart(updatedData.getPeriodStart());
        existing.setPeriodEnd(updatedData.getPeriodEnd());
        existing.setRevenueTargetEur(updatedData.getRevenueTargetEur());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}