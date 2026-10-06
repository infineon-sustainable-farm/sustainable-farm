package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drip_maintenance_logs")
public class DripMaintenanceLog extends BaseEntity {
    @NotNull
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @NotNull
    @Column(name = "maintenance_date", nullable = false)
    private Instant maintenanceDate;

    @NotNull
    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(name = "filter_cleaned")
    private Boolean filterCleaned;

    @Column(name = "clogging_detected")
    private Boolean cloggingDetected;

    @Column(name = "clogging_severity")
    private String cloggingSeverity;

    @Column(name = "emitter_replaced_count")
    private Integer emitterReplacedCount;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "performed_by")
    private String performedBy;

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public Instant getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(Instant maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public Boolean getFilterCleaned() {
        return filterCleaned;
    }

    public void setFilterCleaned(Boolean filterCleaned) {
        this.filterCleaned = filterCleaned;
    }

    public Boolean getCloggingDetected() {
        return cloggingDetected;
    }

    public void setCloggingDetected(Boolean cloggingDetected) {
        this.cloggingDetected = cloggingDetected;
    }

    public String getCloggingSeverity() {
        return cloggingSeverity;
    }

    public void setCloggingSeverity(String cloggingSeverity) {
        this.cloggingSeverity = cloggingSeverity;
    }

    public Integer getEmitterReplacedCount() {
        return emitterReplacedCount;
    }

    public void setEmitterReplacedCount(Integer emitterReplacedCount) {
        this.emitterReplacedCount = emitterReplacedCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}