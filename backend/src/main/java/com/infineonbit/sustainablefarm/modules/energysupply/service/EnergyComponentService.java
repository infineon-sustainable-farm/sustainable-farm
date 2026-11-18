package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service("energyComponentService")
@Transactional(readOnly = true)
public class EnergyComponentService {

    private static final String RELATED_RECORD_CONFLICT =
            "This operation conflicts with a related record. Please refresh and try again.";

    private final EnergyComponentRepository energyComponentRepository;
    private final EntityManager entityManager;
    private final AlertService alertService;

    public EnergyComponentService(EnergyComponentRepository energyComponentRepository, EntityManager entityManager,
                                  AlertService alertService) {
        this.energyComponentRepository = energyComponentRepository;
        this.entityManager = entityManager;
        this.alertService = alertService;
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
        if (energyComponentRepository.existsById(component.getComponentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Energy component already exists: " + component.getComponentId());
        }
        entityManager.persist(component);
        entityManager.flush();
        return EnergyComponentResponse.from(component);
    }

    @Transactional
    public EnergyComponentResponse update(String componentId, EnergyComponentRequest request) {
        EnergyComponent component = findEntity(componentId);
        applyRequest(component, request);
        return EnergyComponentResponse.from(energyComponentRepository.save(component));
    }

    @Transactional
    public void delete(String componentId) {
        EnergyComponent component = findEntity(componentId);
        // The alerts live in the notification module, without a database foreign key to the
        // component: refuse the deletion as the former foreign key did.
        if (alertService.isAboutComponent(componentId)) {
            throw new ConflictException(RELATED_RECORD_CONFLICT);
        }
        energyComponentRepository.delete(component);
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
