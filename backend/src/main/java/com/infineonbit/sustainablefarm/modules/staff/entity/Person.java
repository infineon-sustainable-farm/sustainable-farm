package com.infineonbit.sustainablefarm.modules.staff.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A person of the farm, entered once and referenced by every module (ADR 0004).
 *
 * <p>The roles a person holds in a module (machinery operator, product transformation
 * operator, visitor guide) stay in that module, in a table that references
 * {@code person.id}.</p>
 */
@Entity
@Table(name = "person",
        uniqueConstraints = @UniqueConstraint(name = "person_email_key", columnNames = "email"),
        indexes = @Index(name = "idx_person_full_name", columnList = "full_name"))
@Getter
@Setter
public class Person extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Unique when present. */
    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
