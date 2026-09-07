package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.EquipmentService;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipments")
@AllArgsConstructor
public class EquipmentController {
   private final EquipmentService equipmentService;

   @PostMapping
   public ResponseEntity<EquipmentObtainingResponse> createEquipment(
         @RequestBody EquipmentCreationRequest equipmentCreationRequest) {
      EquipmentObtainingResponse equipmentObtainingResponse = equipmentService.addEquipment(equipmentCreationRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(equipmentObtainingResponse);
   }

   @GetMapping
   public ResponseEntity<List<EquipmentObtainingResponse>> getAllEquipment() {
      List<EquipmentObtainingResponse> listOfEquipmentObtaininResponse = equipmentService.obtainAllEquipments();
      return ResponseEntity.status(HttpStatus.OK).body(listOfEquipmentObtaininResponse);
   }
}
