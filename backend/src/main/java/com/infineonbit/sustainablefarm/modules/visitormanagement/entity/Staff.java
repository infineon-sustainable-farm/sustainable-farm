package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * An entry of the farm people directory, administered by the visitor management Staff screen
 * (resource ownership audit, 2026-11-19), with an optional visitor management role: a guide,
 * receptionist or manager can be assigned to tour time slots (A5); a person with no role is a
 * farm worker (operator, technician) picked by the other modules.
 *
 * <p>Full name, email, phone and the active flag belong to the person of the staff referential
 * ({@code modules.staff.api}, ADR 0004); the role stays here, and the active flag here mirrors
 * the person's. One staff row per person; its foreign key to {@code person} is declared in
 * {@code VisitorStaffForeignKeys}.</p>
 */
@Entity
@Table(name = "staff", uniqueConstraints =
        @UniqueConstraint(name = "staff_person_id_key", columnNames = "person_id"))
public class Staff extends BaseEntity {

    /** Id of the person in the staff referential. */
    @NotNull
    @Column(name = "person_id", nullable = false)
    private Long personId;

    /** Visitor management role; {@code null} for a person who is not visitor staff. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private StaffRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
