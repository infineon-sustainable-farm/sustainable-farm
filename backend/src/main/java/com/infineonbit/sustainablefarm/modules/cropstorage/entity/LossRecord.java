package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A quantity of a batch lost in storage. Top root causes agreed with Anar:
 * overripe mango, mould or moisture, physical damage.
 */
@Entity
@Table(name = "loss_record")
@Getter
@Setter
public class LossRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CropStorageBatch batch;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    private String cause;

    /** Value of the lost stock, in XOF (CFA franc) as entered on the Quality and Loss screen. */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "value_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "value_currency", length = 3))
    private Money value;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime recordedAt;
}
