package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "drip_maintenance_logs", indexes = {
        @Index(name = "idx_drip_maintenance_logs_zone_id", columnList = "zone_id"),
        @Index(name = "idx_drip_maintenance_logs_maintenance_date", columnList = "maintenance_date")
})
public class DripMaintenanceLog extends BaseEntity {
    @NotNull
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

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

    /** Who did the work: a person of the staff referential (docs/adr/0004), by id. */
    @Column(name = "performed_by_person_id")
    private Long performedByPersonId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
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

    public Long getPerformedByPersonId() {
        return performedByPersonId;
    }

    public void setPerformedByPersonId(Long performedByPersonId) {
        this.performedByPersonId = performedByPersonId;
    }
}