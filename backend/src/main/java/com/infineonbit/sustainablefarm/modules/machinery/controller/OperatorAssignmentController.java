package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.OperatorAssignmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.OperatorAssignmentService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the operator assignment management endpoints.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/operator-assignments} — operator assignment creation</li>
 * <li>{@code GET /api/operator-assignments} — paginated list of operator assignments</li>
 * </ul>
 *
 * @since 1.0
 */

@RestController
@RequestMapping("/api/operator-assignments")
@AllArgsConstructor
public class OperatorAssignmentController {
   private final OperatorAssignmentService operatorAssignmentService;

   @PostMapping
   public ResponseEntity<OperatorAssignmentObtainingResponse> createOperatorAssignment(
         @Valid @RequestBody OperatorAssignmentCreationRequest operatorAssignmentCreationRequest) {
      OperatorAssignmentObtainingResponse operatorAssignmentObtainingResponse = operatorAssignmentService
            .addOperatorAssignment(operatorAssignmentCreationRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(operatorAssignmentObtainingResponse);
   }

   @GetMapping
   public ResponseEntity<Page<OperatorAssignmentObtainingResponse>> getAllOperatorAssignments(
         @PageableDefault(size = 10) Pageable pageable) {
      Page<OperatorAssignmentObtainingResponse> operatorAssignments = operatorAssignmentService
            .obtainAllOperatorAssignments(pageable);
      return ResponseEntity.status(HttpStatus.OK).body(operatorAssignments);
   }

   @PatchMapping("/{id}")
   public ResponseEntity<OperatorAssignmentObtainingResponse> updateOperatorAssignment(@PathVariable Long id,
         @RequestBody OperatorAssignmentUpdateRequest operatorAssignmentUpdateRequest) {
      OperatorAssignmentObtainingResponse operatorAssignmentObtainingResponse = operatorAssignmentService
            .updateOperatorAssignment(id, operatorAssignmentUpdateRequest);
      return ResponseEntity.status(HttpStatus.OK).body(operatorAssignmentObtainingResponse);
   }

   @DeleteMapping("/{id}")
   public ResponseEntity<String> deleteOperatorAssignment(@PathVariable Long id) {
      operatorAssignmentService.deleteOperatorAssignment(id);
      return ResponseEntity.status(HttpStatus.OK).body("Operator assignment deleted with Success!");
   }
}