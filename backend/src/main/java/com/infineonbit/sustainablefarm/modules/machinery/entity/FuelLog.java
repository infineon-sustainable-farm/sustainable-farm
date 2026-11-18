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
@Table(name = "fuel_log", indexes = {
        @Index(name = "idx_fuel_log_equipment_id", columnList = "equipment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class FuelLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;

    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal liters;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "cost_amount", precision = 14, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", length = 3, nullable = false))
    private Money cost;

    @Version
    private Long version;
}
