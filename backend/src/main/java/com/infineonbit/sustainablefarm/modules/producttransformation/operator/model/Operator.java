package com.infineonbit.sustainablefarm.modules.producttransformation.operator.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.*;
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
@Table(name = "operator", uniqueConstraints = {
        @UniqueConstraint(name = "uk_operator_code", columnNames = "code"),
        @UniqueConstraint(name = "operator_person_id_key", columnNames = "person_id")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Operator extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
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

}