package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.SparePartObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.exception.SparePartNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SparePartService {
    private final SparePartRepository sparePartRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * Creates a new spare part after validating uniqueness.
     *
     * @param request the creation data (name, quantity, reorderThreshold, unitCost, equipmentId)
     * @return the representation of the created spare part, including its generated ID
     * @throws IllegalArgumentException if a spare part with the same name already exists
     */
    public SparePartObtainingResponse addSparePart(SparePartCreationRequest request) {
        if (sparePartRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Spare part with name '" + request.name() + "' already exists.");
        }

        Equipment equipment = null;
        if (request.equipmentId() != null) {
            equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with id: " + request.equipmentId()));
        }

        SparePart sparePart = new SparePart();
        sparePart.setName(request.name());
        sparePart.setQuantity(request.quantity());
        sparePart.setReorderThreshold(request.reorderThreshold());
        sparePart.setUnitCost(request.unitCost());
        sparePart.setEquipment(equipment);

        try {
            sparePartRepository.save(sparePart);
        } catch (DataIntegrityViolationException e) {
            if (sparePartRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Spare part with name '" + request.name() + "' already exists.");
            }
            if (request.equipmentId() != null) {
                throw new IllegalArgumentException("Equipment not found with id: " + request.equipmentId());
            }
            throw e;
        }

        return toResponse(sparePart);
    }

    /**
     * Retrieves all spare parts (with pagination).
     *
     * @param pageable the pagination information
     * @return the paginated list of spare parts
     */
    public Page<SparePartObtainingResponse> obtainAllSpareParts(Pageable pageable) {
        return sparePartRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single spare part by its ID.
     *
     * @param id the spare part identifier
     * @return the spare part details
     * @throws SparePartNotFoundException if no spare part exists with this ID
     */
    public SparePartObtainingResponse obtainSparePartById(Long id) {
        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new SparePartNotFoundException(id));

        return toResponse(sparePart);
    }

    /**
     * Updates a spare part with new values.
     *
     * @param id the spare part identifier
     * @param request the update data
     * @return the complete state of the spare part after the update
     * @throws SparePartNotFoundException if no spare part exists with this ID
     * @throws IllegalArgumentException if equipment ID is invalid
     */
    @Transactional
    public SparePartObtainingResponse updateSparePart(Long id, SparePartUpdateRequest request) {
        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new SparePartNotFoundException(id));

        String originalName = sparePart.getName();

        if (request.name() != null && !request.name().isEmpty()) {
            if (!request.name().equals(sparePart.getName()) && sparePartRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Spare part with name '" + request.name() + "' already exists.");
            }
            sparePart.setName(request.name());
        }

        if (request.quantity() != null) {
            sparePart.setQuantity(request.quantity());
        }

        if (request.reorderThreshold() != null) {
            sparePart.setReorderThreshold(request.reorderThreshold());
        }

        if (request.unitCost() != null) {
            sparePart.setUnitCost(request.unitCost());
        }

        if (request.equipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with id: " + request.equipmentId()));
            sparePart.setEquipment(equipment);
        }

        try {
            sparePartRepository.save(sparePart);
        } catch (DataIntegrityViolationException e) {
            if (request.name() != null && !request.name().equals(originalName)
                    && sparePartRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Spare part with name '" + request.name() + "' already exists.");
            }
            if (request.equipmentId() != null) {
                throw new IllegalArgumentException("Equipment not found with id: " + request.equipmentId());
            }
            throw e;
        }

        return toResponse(sparePart);
    }

    /**
     * Updates the quantity of a spare part.
     *
     * @param id the spare part identifier
     * @param quantity the new quantity
     * @return the complete state of the spare part after the update
     * @throws SparePartNotFoundException if no spare part exists with this ID
     */
    @Transactional
    public SparePartObtainingResponse updateSparePartQuantity(Long id, Integer quantity) {
        int affectedRows = sparePartRepository.updateSparePartQuantityById(id, quantity);
        if (affectedRows == 0) {
            throw new SparePartNotFoundException(id);
        }

        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new SparePartNotFoundException(id));

        return toResponse(sparePart);
    }

    /**
     * Updates the unit cost of a spare part.
     *
     * @param id the spare part identifier
     * @param unitCost the new unit cost
     * @return the complete state of the spare part after the update
     * @throws SparePartNotFoundException if no spare part exists with this ID
     */
    @Transactional
    public SparePartObtainingResponse updateSparePartUnitCost(Long id, java.math.BigDecimal unitCost) {
        int affectedRows = sparePartRepository.updateSparePartUnitCostById(id, unitCost);
        if (affectedRows == 0) {
            throw new SparePartNotFoundException(id);
        }

        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new SparePartNotFoundException(id));

        return toResponse(sparePart);
    }

    /**
     * Retrieves all spare parts for a specific equipment.
     *
     * @param equipmentId the equipment identifier
     * @return the list of spare parts for the equipment
     */
    public List<SparePartObtainingResponse> obtainSparePartsByEquipment(Long equipmentId) {
        return sparePartRepository.findByEquipmentId(equipmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all spare parts whose quantity is at or below their reorder threshold.
     *
     * @return the list of low stock spare parts, lowest quantity first
     */
    public List<SparePartObtainingResponse> obtainLowStockSpareParts() {
        return sparePartRepository.findLowStockSpareParts().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Permanently deletes a spare part (hard delete).
     *
     * @param id the ID of the spare part to delete
     * @throws SparePartNotFoundException if no spare part exists with this ID
     */
    @Transactional
    public void deleteSparePart(Long id) {
        int affectedRows = sparePartRepository.deleteSparePartById(id);
        if (affectedRows == 0) {
            throw new SparePartNotFoundException(id);
        }
    }

    private SparePartObtainingResponse toResponse(SparePart sparePart) {
        return new SparePartObtainingResponse(
                sparePart.getId(),
                sparePart.getName(),
                sparePart.getQuantity(),
                sparePart.getReorderThreshold(),
                sparePart.getUnitCost(),
                sparePart.getEquipment() != null ? sparePart.getEquipment().getId() : null
        );
    }
}
