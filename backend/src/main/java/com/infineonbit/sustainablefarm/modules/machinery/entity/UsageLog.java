package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "usage_log", indexes = {
        @Index(name = "idx_usage_log_equipment_id", columnList = "equipment_id"),
        @Index(name = "idx_usage_log_operator_person_id", columnList = "operator_person_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UsageLog extends BaseEntity {
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
