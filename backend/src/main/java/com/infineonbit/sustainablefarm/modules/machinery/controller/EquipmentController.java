package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentStatusUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.EquipmentService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing a CRUD system for equipments
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/equipments} — equipment creation</li>
 * <li>{@code GET /api/equipments} — list of equipments</li>
 * <li>{@code PATCH /api/equipments/{id}} — equipment status update</li>
 * <li>{@code DELETE /api/equipments/{id}} — equipment deleting</li>
 * </ul>
 *
 * @since 1.0
 */

@RestController
@RequestMapping("/api/equipments")
@AllArgsConstructor
public class EquipmentController {
   private final EquipmentService equipmentService;

   @PostMapping
   public ResponseEntity<EquipmentObtainingResponse> createEquipment(
         @Valid @RequestBody EquipmentCreationRequest equipmentCreationRequest) {
      EquipmentObtainingResponse equipmentObtainingResponse = equipmentService.addEquipment(equipmentCreationRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(equipmentObtainingResponse);
   }

   @GetMapping
   public ResponseEntity<Page<EquipmentObtainingResponse>> getAllEquipment(@PageableDefault(size = 20)Pageable pageable) {
      Page<EquipmentObtainingResponse> pageOfEquipmentObtaininResponse = equipmentService.obtainAllEquipments(pageable);
      return ResponseEntity.status(HttpStatus.OK).body(pageOfEquipmentObtaininResponse);
   }

   @PatchMapping("/{id}")
   public ResponseEntity<EquipmentObtainingResponse> updateEquipmentStatus(@PathVariable Long id,
         @Valid @RequestBody EquipmentStatusUpdateRequest equipmentStatusUpdateRequest) {
      EquipmentObtainingResponse equipmentObtainingResponse = equipmentService.updateEquipmentStatus(id,
            equipmentStatusUpdateRequest);
      return ResponseEntity.status(HttpStatus.OK).body(equipmentObtainingResponse);
   }

   @DeleteMapping("/{id}")
   public ResponseEntity<String> deleteEquipment(@PathVariable Long id) {
      equipmentService.deleteEquipment(id);
      return ResponseEntity.status(HttpStatus.OK).body("Equipment deleted with Success!");
   }
}
