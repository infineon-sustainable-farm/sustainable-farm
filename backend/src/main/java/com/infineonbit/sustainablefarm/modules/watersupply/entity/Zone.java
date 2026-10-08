package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "zones")
public class Zone extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID fieldId;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(nullable = false)
    private Double areaHectares;

    private String irrigationMethod;

    /**
     * Crop coefficient (Kc) used to estimate the zone's theoretical water need
     * (need = area x ET0 x Kc / system efficiency). Null = default value 1.0,
     * which is still more accurate than no reference at all.
     */
    @Column(name = "crop_coefficient")
    private Double cropCoefficient;

    /**
     * Number of emitters of the zone and nominal flow of one emitter (L/h): they give the
     * network's theoretical flow, the reference for clogging detection (measured flow < 90 %)
     * or leak detection (measured flow > 110 %).
     */
    @Column(name = "emitter_count")
    private Integer emitterCount;

    @Column(name = "emitter_nominal_flow_lh")
    private Double emitterNominalFlowLh;

    /** Theoretical flow of the zone's network (L/h), or null when the network is not described. */
    public Double theoreticalFlowLitersPerHour() {
        if (emitterCount == null || emitterCount <= 0 || emitterNominalFlowLh == null || emitterNominalFlowLh <= 0) {
            return null;
        }
        return emitterCount * emitterNominalFlowLh;
    }

    public UUID getFieldId() {
        return fieldId;
    }

    public void setFieldId(UUID fieldId) {
        this.fieldId = fieldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAreaHectares() {
        return areaHectares;
    }

    public void setAreaHectares(Double areaHectares) {
        this.areaHectares = areaHectares;
    }

    public String getIrrigationMethod() {
        return irrigationMethod;
    }

    public void setIrrigationMethod(String irrigationMethod) {
        this.irrigationMethod = irrigationMethod;
    }

    public Double getCropCoefficient() {
        return cropCoefficient;
    }

    public void setCropCoefficient(Double cropCoefficient) {
        this.cropCoefficient = cropCoefficient;
    }

    public Integer getEmitterCount() {
        return emitterCount;
    }

    public void setEmitterCount(Integer emitterCount) {
        this.emitterCount = emitterCount;
    }

    public Double getEmitterNominalFlowLh() {
        return emitterNominalFlowLh;
    }

    public void setEmitterNominalFlowLh(Double emitterNominalFlowLh) {
        this.emitterNominalFlowLh = emitterNominalFlowLh;
    }
}