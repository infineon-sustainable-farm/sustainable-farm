package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentService {

    private final ShipmentRepository repository;

    public ShipmentService(ShipmentRepository repository) {
        this.repository = repository;
    }

    public List<Shipment> getAll() {
        return repository.findAll();
    }

    public Shipment getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shipment not found with id: " + id));
    }

    public Shipment create(Shipment item) {
        return repository.save(item);
    }

    public Shipment update(Integer id, Shipment updatedData) {
        Shipment existing = getById(id);

        existing.setShipmentCode(updatedData.getShipmentCode());
        existing.setOrderId(updatedData.getOrderId());
        existing.setOrigin(updatedData.getOrigin());
        existing.setDestination(updatedData.getDestination());
        existing.setStatus(updatedData.getStatus());
        existing.setEta(updatedData.getEta());
        existing.setVesselName(updatedData.getVesselName());
        existing.setVoyageNumber(updatedData.getVoyageNumber());
        existing.setDelayReason(updatedData.getDelayReason());
        existing.setCurrentLat(updatedData.getCurrentLat());
        existing.setCurrentLon(updatedData.getCurrentLon());
        existing.setSpeedKnots(updatedData.getSpeedKnots());
        existing.setCurrentLocationLabel(updatedData.getCurrentLocationLabel());
        existing.setDistanceRemainingNm(updatedData.getDistanceRemainingNm());
        existing.setCommodityDescription(updatedData.getCommodityDescription());
        existing.setContainerCount(updatedData.getContainerCount());
        existing.setContainerType(updatedData.getContainerType());
        existing.setCargoWeightKg(updatedData.getCargoWeightKg());
        existing.setCurrentTempC(updatedData.getCurrentTempC());
        existing.setTempStatus(updatedData.getTempStatus());
        existing.setManifestUrl(updatedData.getManifestUrl());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}