package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

/** The figures of a campaign on a day, deleted with the campaign. */
@Entity
@Table(name = "campaign_metrics", indexes = @Index(name = "idx_campaign_metrics_camp", columnList = "campaign_id"))
@Getter
@Setter
public class CampaignMetric extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false,
            foreignKey = @ForeignKey(name = "campaign_metrics_campaign_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Campaign campaign;

    @Column(name = "metric_date")
    private LocalDate metricDate;

    @ColumnDefault("0")
    @Column(name = "clicks")
    private Integer clicks;

    @ColumnDefault("0")
    @Column(name = "signups")
    private Integer signups;

    @ColumnDefault("0")
    @Column(name = "reach")
    private Integer reach;
}
