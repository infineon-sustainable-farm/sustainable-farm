package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnergyComponentService {

    private final EnergyComponentRepository repository;

    @Autowired
    public EnergyComponentService(EnergyComponentRepository repository) {
        this.repository = repository;
    }

    public List<EnergyComponent> findAll() {
        return repository.findAll();
    }

    public Optional<EnergyComponent> findById(String componentId) {
        return repository.findById(componentId);
    }

    public EnergyComponent save(EnergyComponent component) {
        return repository.save(component);
    }

    public void deleteById(String componentId) {
        repository.deleteById(componentId);
    }
}
