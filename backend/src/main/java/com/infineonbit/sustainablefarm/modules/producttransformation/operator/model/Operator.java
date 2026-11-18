package com.infineonbit.sustainablefarm.modules.producttransformation.operator.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

/**
 * Operator Entity: the product transformation role of a person of the staff referential
 * (ADR 0004). The person ({@code person_id}, foreign key in
 * {@code ProductTransformationPersonForeignKeys}) carries the name; the role, certifications,
 * hire date and active status stay here (deactivating an operator leaves the person's other
 * roles as they are).
 *
 * <p>{@code operatorName} is not stored in this table: it is the person's name, filled by
 * {@code OperatorServiceImpl} when it returns an operator, and carries the requested name when
 * an operator is created or updated.</p>
 *
 * Business Rule: Operators must be certified for assigned role
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "operator", uniqueConstraints =
        @UniqueConstraint(name = "operator_person_id_key", columnNames = "person_id"))
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    @Id
    @GeneratedValue(generator = "assigned")
    @GenericGenerator(name = "assigned", strategy = "assigned")
    @Column(name = "operator_id", length = 50)
    private String operatorId;

    /** The person of the staff referential ({@code person.id}). */
    @Column(name = "person_id", nullable = false)
    private Long personId;

    /** The person's full name (not stored here). */
    @Transient
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
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

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