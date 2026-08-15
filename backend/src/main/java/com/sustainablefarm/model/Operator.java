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
 * Operator Entity
 * Personnel data - Supporting Entity
 * 
 * Business Rule: Operators must be certified for assigned role
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "operator")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    @Id
    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Column(name = "certifications", columnDefinition = "TEXT")
    private String certifications;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_status", nullable = false, length = 20)
    private ActiveStatus activeStatus = ActiveStatus.ACTIVE;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

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
     * Role Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum Role {
        WASHER,
        DRYER,
        PACKAGER,
        QC_INSPECTOR,
        SUPERVISOR,
        AUDITOR
    }

    /**
     * Active Status Enum
     * Business Rule: Only ACTIVE operators can be assigned
     */
    public enum ActiveStatus {
        ACTIVE,
        INACTIVE
    }
}