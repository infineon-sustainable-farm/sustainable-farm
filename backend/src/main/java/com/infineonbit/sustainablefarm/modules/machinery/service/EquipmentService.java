package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;

    public EquipmentObtainingResponse addEquipment(EquipmentCreationRequest equipmentCreationRequest) {
        String clientSendingEquipmentName = equipmentCreationRequest.name();
        if (equipmentRepository.existsByName(clientSendingEquipmentName)){
            throw new IllegalArgumentException(clientSendingEquipmentName+" already exists.");
        }
        Equipment equipment = new Equipment();
        equipment.setName(equipmentCreationRequest.name());
        equipment.setCategory(equipmentCreationRequest.category());
        equipment.setStage(equipmentCreationRequest.stage());
        equipment.setStatus(equipmentCreationRequest.status());
        equipmentRepository.save(equipment);
        return new EquipmentObtainingResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getStage(),
                equipment.getStatus());
    }

    public List<EquipmentObtainingResponse> obtainAllEquipments() {
        List<EquipmentObtainingResponse> listOfEquipmentsResponse = new ArrayList<>();
        List<Equipment> listOfEquipments = equipmentRepository.findAll();
        for(int i = 0; i < listOfEquipments.size(); i++){
            EquipmentObtainingResponse equipmentObtainingResponse = new EquipmentObtainingResponse(
                    listOfEquipments.get(i).getId(),
                    listOfEquipments.get(i).getName(),
                    listOfEquipments.get(i).getCategory(),
                    listOfEquipments.get(i).getStage(),
                    listOfEquipments.get(i).getStatus()
            );
            listOfEquipmentsResponse.add(equipmentObtainingResponse);
        }
        return listOfEquipmentsResponse;
    }

}
