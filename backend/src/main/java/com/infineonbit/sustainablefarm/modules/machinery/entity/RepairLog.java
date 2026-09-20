package com.infineonbit.sustainablefarm.modules.machinery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repair_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepairLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(length = 60)
    private String technician;

    @Version
    private Long version;
}
