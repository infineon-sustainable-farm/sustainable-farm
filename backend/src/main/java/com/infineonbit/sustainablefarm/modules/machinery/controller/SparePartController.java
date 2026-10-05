package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.SparePartObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.SparePartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machinery/spare-parts")
@AllArgsConstructor
public class SparePartController {
    private final SparePartService sparePartService;

    /**
     * Create a new spare part.
     *
     * @param request the creation data
     * @return the created spare part
     */
    @PostMapping
    public ResponseEntity<SparePartObtainingResponse> createSparePart(@Valid @RequestBody SparePartCreationRequest request) {
        SparePartObtainingResponse response = sparePartService.addSparePart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all spare parts (paginated).
     *
     * @param page the page number (default 0)
     * @param size the number of items per page (default 10)
     * @param sortBy the field to sort by (default id)
     * @param sortDirection the sort direction (default asc)
     * @return the paginated list of spare parts
     */
    @GetMapping
    public ResponseEntity<Page<SparePartObtainingResponse>> getAllSpareParts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SparePartObtainingResponse> response = sparePartService.obtainAllSpareParts(pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all spare parts at or below their reorder threshold.
     *
     * @return the list of low stock spare parts
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<SparePartObtainingResponse>> getLowStockSpareParts() {
        List<SparePartObtainingResponse> response = sparePartService.obtainLowStockSpareParts();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single spare part by ID.
     *
     * @param id the spare part ID
     * @return the spare part details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SparePartObtainingResponse> getSparePartById(@PathVariable Long id) {
        SparePartObtainingResponse response = sparePartService.obtainSparePartById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a spare part (full update).
     *
     * @param id the spare part ID
     * @param request the update data
     * @return the updated spare part
     */
    @PutMapping("/{id}")
    public ResponseEntity<SparePartObtainingResponse> updateSparePart(
            @PathVariable Long id,
            @Valid @RequestBody SparePartUpdateRequest request) {
        SparePartObtainingResponse response = sparePartService.updateSparePart(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the quantity of a spare part (partial update).
     *
     * @param id the spare part ID
     * @param quantity the new quantity
     * @return the updated spare part
     */
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<SparePartObtainingResponse> updateSparePartQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        SparePartObtainingResponse response = sparePartService.updateSparePartQuantity(id, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the unit cost of a spare part (partial update).
     *
     * @param id the spare part ID
     * @param unitCost the new unit cost
     * @return the updated spare part
     */
    @PatchMapping("/{id}/unit-cost")
    public ResponseEntity<SparePartObtainingResponse> updateSparePartUnitCost(
            @PathVariable Long id,
            @RequestParam java.math.BigDecimal unitCost) {
        SparePartObtainingResponse response = sparePartService.updateSparePartUnitCost(id, unitCost);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all spare parts for a specific equipment.
     *
     * @param equipmentId the equipment ID
     * @return the list of spare parts for the equipment
     */
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<SparePartObtainingResponse>> getSparePartsByEquipment(@PathVariable Long equipmentId) {
        List<SparePartObtainingResponse> response = sparePartService.obtainSparePartsByEquipment(equipmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a spare part.
     *
     * @param id the spare part ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSparePart(@PathVariable Long id) {
        sparePartService.deleteSparePart(id);
        return ResponseEntity.noContent().build();
    }
}
