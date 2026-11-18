package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance_schedule", indexes = {
        @Index(name = "idx_maintenance_schedule_equipment_id", columnList = "equipment_id"),
        @Index(name = "idx_maintenance_schedule_operator_person_id", columnList = "operator_person_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MaintenanceType type;

    @Column(length = 100)
    private String frequency;

    @Column(nullable = false)
    private LocalDate lastCompleted;

    @Column(nullable = false)
    private LocalDate nextDue;

    /** The operator, a person of the staff referential ({@code person.id}); the API shows the name. */
    @Column(name = "operator_person_id")
    private Long operatorPersonId;

    @Version
    private Long version;
}
