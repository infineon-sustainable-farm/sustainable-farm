package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentStatusUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;

    /**
     * Equipments Management Service
     * <p>Equipment Name Normalization method
     * <ul>
     *      <li>Equipment name is always normalize before insertion in DB</li>
     *      <li>Regardless of the format, a piece of equipment will always be created using the format defined by this method.</li>
     *      <li>Example: If client gives "tRActor" it becomes "Tractor"</li>
     * </ul>
     */
    private static String normalizeEquipmentName(String name){
		name = name.trim();
		String lowerCaseName = name.toLowerCase();
		char[] nameStartCharArray = lowerCaseName.substring(0,1).toUpperCase().toCharArray();
		char[] nameCharArray = lowerCaseName.toCharArray();

		nameCharArray[0] = nameStartCharArray[0];
		String normalizeName = new String (nameCharArray);
		return normalizeName;
	}

    /**
     * Creates a new piece of equipment after validating uniqueness.
     *
     * <p>The name is normalized to title case before insertion, ensuring
     * case-insensitive uniqueness (e.g., "tRactor" → "Tractor").
     *
     * @param request the creation data (name, category, stage, status)
     * @return the representation of the created equipment, including its generated ID
     * @throws IllegalArgumentException if a piece of equipment with the same name (case-insensitive) already exists
     */
    public EquipmentObtainingResponse addEquipment(EquipmentCreationRequest equipmentCreationRequest) {

        String clientSendingEquipmentName = normalizeEquipmentName(equipmentCreationRequest.name());
        if (equipmentRepository.existsByName(clientSendingEquipmentName)){
            throw new IllegalArgumentException(clientSendingEquipmentName+" already exists.");
        }
        Equipment equipment = new Equipment();
        equipment.setName(clientSendingEquipmentName);
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

    /**
     * Retrieves all equipment (with pagination).
     *
     * <p>Don't returns the entire table.
     * Returns a 20 paginated objects.
     *
     * @return the 20 paginated list of equipment
     */
    public Page<EquipmentObtainingResponse> obtainAllEquipments(Pageable pageable) {
        Page<Equipment> pageOfEquipments = equipmentRepository.findAll(pageable);
        return pageOfEquipments.map(equipment -> new EquipmentObtainingResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getStage(),
                equipment.getStatus()
        ));
    }

    /**
     * Updates the status of a piece of equipment (partial update).
     *
     * <p>Only the {@code status} field is modified; the other fields
     * (name, category, stage) remain unchanged.
     *
     * @param id      the equipment identifier
     * @param request the new status
     * @return the complete state of the equipment after the update
     * @throws EquipmentNotFoundException if no equipment exists with this ID
     */
    public EquipmentObtainingResponse updateEquipmentStatus(Long equipmentToUpdateId , EquipmentStatusUpdateRequest equipmentStatusUpdateRequest){
        int affectedRow = equipmentRepository.updateEquipmentStatusById(equipmentToUpdateId, equipmentStatusUpdateRequest.status());
         if(affectedRow == 0){
             throw new EquipmentNotFoundException(equipmentToUpdateId);
         }
        Optional<Equipment> jpaEquipment = equipmentRepository.findById(equipmentToUpdateId);
        Equipment equipment = jpaEquipment.get();
        return new EquipmentObtainingResponse(equipment.getId(), equipment.getName(), equipment.getCategory(), equipment.getStage(), equipment.getStatus() );
    }

    /**
     * Permanently deletes a piece of equipment (hard delete, not soft delete).
     *
     * @param id the ID of the equipment to delete
     * @throws EquipmentNotFoundException if no equipment exists with this ID
     */
    @Transactional
    public void deleteEquipment(Long id){
        Optional<Equipment> equipment = equipmentRepository.findById(id);
        if(equipment.isEmpty()){
            throw new EquipmentNotFoundException(id);
        }
        equipmentRepository.deleteById(id);
    }

}
