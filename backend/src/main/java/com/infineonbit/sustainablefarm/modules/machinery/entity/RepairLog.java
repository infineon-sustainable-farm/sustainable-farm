package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repair_log", indexes = {
        @Index(name = "idx_repair_log_equipment_id", columnList = "equipment_id"),
        @Index(name = "idx_repair_log_technician_person_id", columnList = "technician_person_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RepairLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;

    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 500)
    private String issue;

    @Column(precision = 10, scale = 2)
    private BigDecimal downtime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "cost_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", length = 3))
    private Money cost;

    /** The technician, a person of the staff referential ({@code person.id}); the API shows the name. */
    @Column(name = "technician_person_id")
    private Long technicianPersonId;

    @Version
    private Long version;
}
