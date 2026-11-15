package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesChannelService {

    private final SalesChannelRepository repository;

    public SalesChannelService(SalesChannelRepository repository) {
        this.repository = repository;
    }

    public List<SalesChannel> getAll() {
        return repository.findAll();
    }

    public SalesChannel getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SalesChannel not found with id: " + id));
    }

    public SalesChannel create(SalesChannel item) {
        return repository.save(item);
    }

    public SalesChannel update(Integer id, SalesChannel updatedData) {
        SalesChannel existing = getById(id);
        existing.setName(updatedData.getName());
        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}