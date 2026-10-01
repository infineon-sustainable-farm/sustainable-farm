package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.FieldService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FieldController {
    private final FieldService fieldService;

    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @GetMapping("/farms/{farmId}/fields")
    public Object farmFields(
            @PathVariable UUID farmId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return fieldService.findFarmFields(farmId);
        }
        return fieldService.findFarmFields(farmId, pageRequest(page, size));
    }

    @GetMapping("/fields")
    public Object fields(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId) {
        if (page == null && size == null && farmId == null) {
            return fieldService.findFields();
        }
        PageRequest pageable = pageRequest(page, size);
        return farmId == null ? fieldService.findFields(pageable) : fieldService.findFarmFields(farmId, pageable);
    }

    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponse createField(@Valid @RequestBody FieldCreateRequest request) {
        return fieldService.createField(request);
    }

    @GetMapping("/fields/{fieldId}")
    public FieldResponse field(@PathVariable UUID fieldId) {
        return fieldService.getField(fieldId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}