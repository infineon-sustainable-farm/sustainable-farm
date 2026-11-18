package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
