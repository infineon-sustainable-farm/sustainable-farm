package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.EducationalProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EducationalProgramController {

    private final EducationalProgramService programService;

    public EducationalProgramController(EducationalProgramService programService) {
        this.programService = programService;
    }

    @GetMapping("/tour-stops")
    public List<TourStopResponse> stops() {
        return programService.listStops();
    }

    @PostMapping("/tour-stops")
    public ResponseEntity<TourStopResponse> createStop(@Valid @RequestBody TourStopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.createStop(request));
    }

    @PutMapping("/tour-stops/{id}")
    public TourStopResponse updateStop(@PathVariable Long id, @Valid @RequestBody TourStopRequest request) {
        return programService.updateStop(id, request);
    }

    @DeleteMapping("/tour-stops/{id}")
    public ResponseEntity<Void> deactivateStop(@PathVariable Long id) {
        programService.deactivateStop(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workshops")
    public List<WorkshopResponse> workshops(@RequestParam(required = false) WorkshopStatus status) {
        return programService.listWorkshops(status);
    }

    @PostMapping("/workshops")
    public ResponseEntity<WorkshopResponse> createWorkshop(@Valid @RequestBody WorkshopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.createWorkshop(request));
    }

    @PutMapping("/workshops/{id}")
    public WorkshopResponse updateWorkshop(@PathVariable Long id, @Valid @RequestBody WorkshopRequest request) {
        return programService.updateWorkshop(id, request);
    }

    @PostMapping("/workshops/{id}/publish")
    public WorkshopResponse publishWorkshop(@PathVariable Long id) {
        return programService.publishWorkshop(id);
    }

    @PostMapping("/workshops/{id}/deactivate")
    public WorkshopResponse deactivateWorkshop(@PathVariable Long id) {
        return programService.deactivateWorkshop(id);
    }
}