package com.infineonbit.sustainablefarm.modules.machinery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "spare_part")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SparePart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reorderThreshold;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal unitCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Equipment equipment;
}
