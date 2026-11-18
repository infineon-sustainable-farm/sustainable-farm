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
 * A farm staff member or guide who can be assigned to tour time slots
 * (A5: staff / guides).
 *
 * <p>The visitor management extension of a person of the staff referential
 * ({@code modules.staff.api}, ADR 0004): full name, email and phone belong to the person;
 * the role and the active flag (a guide can be deactivated without touching the person's other
 * roles) stay here. One staff row per person; its foreign key
 * to {@code person} is declared in {@code VisitorStaffForeignKeys}.</p>
 */
@Entity
@Table(name = "staff", uniqueConstraints =
        @UniqueConstraint(name = "staff_person_id_key", columnNames = "person_id"))
public class Staff extends BaseEntity {

    /** Id of the person in the staff referential. */
    @NotNull
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private StaffRole role = StaffRole.GUIDE;

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
