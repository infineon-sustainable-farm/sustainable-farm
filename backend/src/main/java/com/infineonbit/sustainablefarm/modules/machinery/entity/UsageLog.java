package com.infineonbit.sustainablefarm.modules.machinery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "usage_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;

    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hoursUsed;

    /** The operator, a person of the staff referential ({@code person.id}); the API shows the name. */
    @Column(name = "operator_person_id")
    private Long operatorPersonId;

    @Column(length = 500)
    private String notes;

    @Version
    private Long version;
}
