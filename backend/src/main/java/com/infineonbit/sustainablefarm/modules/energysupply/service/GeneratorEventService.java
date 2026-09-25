package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GeneratorEventService {

    private final GeneratorEventRepository repository;

    @Autowired
    public GeneratorEventService(GeneratorEventRepository repository) {
        this.repository = repository;
    }

    public List<GeneratorEvent> findAll() {
        return repository.findAll();
    }

    public Optional<GeneratorEvent> findById(String eventId) {
        return repository.findById(eventId);
    }

    public GeneratorEvent save(GeneratorEvent event) {
        return repository.save(event);
    }

    public void deleteById(String eventId) {
        repository.deleteById(eventId);
    }
}
