package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FarmController {
    private final FarmRepository farmRepository;
    private final FieldRepository fieldRepository;
    private final ZoneRepository zoneRepository;

    public FarmController(FarmRepository farmRepository, FieldRepository fieldRepository, ZoneRepository zoneRepository) {
        this.farmRepository = farmRepository;
        this.fieldRepository = fieldRepository;
        this.zoneRepository = zoneRepository;
    }

    @GetMapping("/farms")
    public List<Farm> farms() {
        return farmRepository.findAll();
    }

    @PostMapping("/farms")
    @ResponseStatus(HttpStatus.CREATED)
    public Farm createFarm(@Valid @RequestBody Farm farm) {
        return farmRepository.save(farm);
    }

    @GetMapping("/farms/{farmId}")
    public Farm farm(@PathVariable UUID farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new NotFoundException("Farm"));
    }

    @PutMapping("/farms/{farmId}")
    public Farm updateFarm(@PathVariable UUID farmId, @RequestBody Farm payload) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new NotFoundException("Farm"));
        farm.setName(payload.getName() == null ? farm.getName() : payload.getName());
        farm.setDescription(payload.getDescription());
        farm.setAddress(payload.getAddress());
        farm.setLatitude(payload.getLatitude());
        farm.setLongitude(payload.getLongitude());
        farm.setAreaHectares(payload.getAreaHectares());
        return farmRepository.save(farm);
    }

    @DeleteMapping("/farms/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFarm(@PathVariable UUID farmId) {
        farmRepository.delete(farmRepository.findById(farmId).orElseThrow(() -> new NotFoundException("Farm")));
    }

    @GetMapping("/farms/{farmId}/fields")
    public List<Field> farmFields(@PathVariable UUID farmId) {
        return fieldRepository.findByFarmId(farmId);
    }

    @GetMapping("/fields")
    public List<Field> fields() {
        return fieldRepository.findAll();
    }

    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public Field createField(@Valid @RequestBody Field field) {
        return fieldRepository.save(field);
    }

    @GetMapping("/fields/{fieldId}")
    public Field field(@PathVariable UUID fieldId) {
        return fieldRepository.findById(fieldId).orElseThrow(() -> new NotFoundException("Field"));
    }

    @PutMapping("/fields/{fieldId}")
    public Field updateField(@PathVariable UUID fieldId, @RequestBody Field payload) {
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new NotFoundException("Field"));
        field.setFarmId(payload.getFarmId() == null ? field.getFarmId() : payload.getFarmId());
        field.setName(payload.getName() == null ? field.getName() : payload.getName());
        field.setAreaHectares(payload.getAreaHectares() == null ? field.getAreaHectares() : payload.getAreaHectares());
        field.setCropType(payload.getCropType());
        field.setSoilType(payload.getSoilType());
        field.setCoordinates(payload.getCoordinates());
        return fieldRepository.save(field);
    }

    @DeleteMapping("/fields/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteField(@PathVariable UUID fieldId) {
        fieldRepository.delete(fieldRepository.findById(fieldId).orElseThrow(() -> new NotFoundException("Field")));
    }

    @GetMapping("/fields/{fieldId}/zones")
    public List<Zone> fieldZones(@PathVariable UUID fieldId) {
        return zoneRepository.findByFieldId(fieldId);
    }

    @GetMapping("/zones")
    public List<Zone> zones() {
        return zoneRepository.findAll();
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public Zone createZone(@Valid @RequestBody Zone zone) {
        return zoneRepository.save(zone);
    }

    @GetMapping("/zones/{zoneId}")
    public Zone zone(@PathVariable UUID zoneId) {
        return zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone"));
    }

    @PutMapping("/zones/{zoneId}")
    public Zone updateZone(@PathVariable UUID zoneId, @RequestBody Zone payload) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone"));
        zone.setFieldId(payload.getFieldId() == null ? zone.getFieldId() : payload.getFieldId());
        zone.setName(payload.getName() == null ? zone.getName() : payload.getName());
        zone.setAreaHectares(payload.getAreaHectares() == null ? zone.getAreaHectares() : payload.getAreaHectares());
        zone.setIrrigationMethod(payload.getIrrigationMethod());
        return zoneRepository.save(zone);
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZone(@PathVariable UUID zoneId) {
        zoneRepository.delete(zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone")));
    }
}
