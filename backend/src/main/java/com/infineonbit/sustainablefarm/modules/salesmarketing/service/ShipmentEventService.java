package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentEventService {

    private final ShipmentEventRepository repository;

    public ShipmentEventService(ShipmentEventRepository repository) {
        this.repository = repository;
    }

    public List<ShipmentEvent> getAll() {
        return repository.findAll();
    }

    public ShipmentEvent getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ShipmentEvent not found with id: " + id));
    }

    public ShipmentEvent create(ShipmentEvent item) {
        return repository.save(item);
    }

    public ShipmentEvent update(Integer id, ShipmentEvent updatedData) {
        ShipmentEvent existing = getById(id);

        existing.setShipmentId(updatedData.getShipmentId());
        existing.setStep(updatedData.getStep());
        existing.setEventTime(updatedData.getEventTime());
        existing.setNote(updatedData.getNote());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}