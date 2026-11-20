package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** The revenue target of a sales channel over a period, in euros. */
@Entity
@Table(name = "sales_channel_targets", indexes = @Index(name = "idx_channel_targets_channel", columnList = "channel_id, period_start"))
@Getter
@Setter
public class SalesChannelTarget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false, foreignKey = @ForeignKey(name = "sales_channel_targets_channel_id_fkey"))
    private SalesChannel channel;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "revenue_target_amount", nullable = false, precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "revenue_target_currency", nullable = false, length = 3))
    private Money revenueTarget;
}
