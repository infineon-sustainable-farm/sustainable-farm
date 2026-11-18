package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(
        name = "operator_assignment",
        indexes = @Index(name = "idx_operator_assignment_person_id", columnList = "person_id"),
        check = @CheckConstraint(constraint = "end_date IS NULL OR end_date >= start_date"))
@Getter
@Setter
@NoArgsConstructor
public class OperatorAssignment extends BaseEntity {
    /** The operator: a person of the staff referential ({@code person.id}, see MachineryPersonForeignKeys). */
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @OneToOne
    @JoinColumn(name="equipment_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Equipment equipment;

    @Column(length = 60, nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = true)
    private LocalDate endDate;

    @Version
    private Long version;

    public OperatorAssignment(Long id, Long personId, Equipment equipment, String jobTitle,
                              LocalDate startDate, LocalDate endDate, Long version) {
        setId(id);
        this.personId = personId;
        this.equipment = equipment;
        this.jobTitle = jobTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.version = version;
    }
}