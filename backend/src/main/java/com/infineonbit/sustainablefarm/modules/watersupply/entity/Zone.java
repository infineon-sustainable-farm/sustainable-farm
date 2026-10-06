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
     * Coefficient cultural (Kc) utilisé pour estimer le besoin hydrique théorique de la zone
     * (besoin = surface x ET0 x Kc / efficacité du système). Null = valeur par défaut 1.0,
     * ce qui reste plus juste qu'aucune référence du tout.
     */
    @Column(name = "crop_coefficient")
    private Double cropCoefficient;

    /**
     * Nombre de goutteurs de la zone et débit nominal d'un goutteur (L/h) : ils donnent le
     * débit théorique du réseau, référence de la détection de colmatage (débit mesuré < 90 %)
     * ou de fuite (débit mesuré > 110 %).
     */
    @Column(name = "emitter_count")
    private Integer emitterCount;

    @Column(name = "emitter_nominal_flow_lh")
    private Double emitterNominalFlowLh;

    /** Débit théorique du réseau de la zone (L/h), ou null si le réseau n'est pas décrit. */
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