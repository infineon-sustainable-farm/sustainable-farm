package com.infineonbit.sustainablefarm.modules.machinery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FuelLog {
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
    private BigDecimal liters;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Version
    private Long version;
}
