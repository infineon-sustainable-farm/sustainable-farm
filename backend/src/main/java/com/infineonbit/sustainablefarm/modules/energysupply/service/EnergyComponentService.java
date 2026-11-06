package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("energyComponentService")
@Transactional(readOnly = true)
public class EnergyComponentService {

    private final EnergyComponentRepository energyComponentRepository;

    public EnergyComponentService(EnergyComponentRepository energyComponentRepository) {
        this.energyComponentRepository = energyComponentRepository;
    }

    public List<EnergyComponentResponse> findAll() {
        return energyComponentRepository.findAll().stream().map(EnergyComponentResponse::from).toList();
    }

    public EnergyComponentResponse findById(String componentId) {
        return EnergyComponentResponse.from(findEntity(componentId));
    }

    @Transactional
    public EnergyComponentResponse create(EnergyComponentRequest request) {
        EnergyComponent component = new EnergyComponent();
        component.setComponentId(request.componentId());
        applyRequest(component, request);
        return EnergyComponentResponse.from(energyComponentRepository.save(component));
    }

    @Transactional
    public EnergyComponentResponse update(String componentId, EnergyComponentRequest request) {
        EnergyComponent component = findEntity(componentId);
        applyRequest(component, request);
        return EnergyComponentResponse.from(energyComponentRepository.save(component));
    }

    @Transactional
    public void delete(String componentId) {
        energyComponentRepository.delete(findEntity(componentId));
    }

    private EnergyComponent findEntity(String componentId) {
        return energyComponentRepository.findById(componentId)
                .orElseThrow(() -> new EnergySupplyNotFoundException("Energy component", componentId));
    }

    private void applyRequest(EnergyComponent component, EnergyComponentRequest request) {
        component.setName(request.name());
        component.setCategory(request.category());
        component.setCapacityValue(request.capacityValue());
        component.setStatus(request.status());
    }
}
