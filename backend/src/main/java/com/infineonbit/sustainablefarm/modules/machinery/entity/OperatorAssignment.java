package com.infineonbit.sustainablefarm.modules.machinery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(
        name = "operator_assignment",
        check = @CheckConstraint(constraint = "end_date IS NULL OR end_date >= start_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatorAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false)
    private String fullName;

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
}