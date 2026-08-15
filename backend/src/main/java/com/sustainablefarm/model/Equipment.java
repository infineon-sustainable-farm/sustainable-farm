package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

/**
 * Equipment Entity
 * Machinery and equipment data - Supporting Entity
 * 
 * Business Rule: Equipment must be available before assignment
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "equipment")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    @Id
    @Column(name = "equipment_id", length = 50)
    private String equipmentId;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 50)
    private EquipmentType equipmentType;

    @Column(name = "capacity_kg_per_hour", nullable = false, precision = 10, scale = 2)
    private Double capacityKgPerHour;

    @Column(name = "energy_consumption_kwh_per_kg", nullable = false, precision = 10, scale = 4)
    private Double energyConsumptionKwhPerKg;

    @Column(name = "location", length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_status", nullable = false, length = 20)
    private MaintenanceStatus maintenanceStatus = MaintenanceStatus.ACTIVE;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "created_at", updatable = false)
    private java.time.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.time.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    /**
     * Equipment Type Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum EquipmentType {
        WASHING,
        DRYING,
        PACKAGING,
        TESTA_DRYER,
        SOLAR_DRYER,
        TUNNEL_DRYER
    }

    /**
     * Maintenance Status Enum
     * Business Rule: Equipment must be ACTIVE for assignment
     */
    public enum MaintenanceStatus {
        ACTIVE,
        MAINTENANCE,
        RETIRED
    }
}