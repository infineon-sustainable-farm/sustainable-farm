package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

/** A marketing campaign. Its budget is in euros; {@code currency} is the label the API keeps. */
@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** {@code scheduled} when none is sent. */
    @ColumnDefault("'scheduled'")
    @Convert(converter = CampaignStatus.Converter.class)
    @Column(name = "status", nullable = false, length = 20)
    private CampaignStatus status;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "budget_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "budget_currency", length = 3))
    private Money budget;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "budget_spent_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "budget_spent_currency", length = 3))
    private Money budgetSpent;

    @Column(name = "estimated_reach")
    private Integer estimatedReach;

    @ColumnDefault("'EUR'")
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
