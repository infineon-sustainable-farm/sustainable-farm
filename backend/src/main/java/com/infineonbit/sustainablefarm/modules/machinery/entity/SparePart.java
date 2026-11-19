package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "spare_part", indexes = {
        @Index(name = "idx_spare_part_equipment_id", columnList = "equipment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SparePart extends BaseEntity {
    @Column(length = 255, nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reorderThreshold;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_cost_amount", precision = 14, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_cost_currency", length = 3, nullable = false))
    private Money unitCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;
}
