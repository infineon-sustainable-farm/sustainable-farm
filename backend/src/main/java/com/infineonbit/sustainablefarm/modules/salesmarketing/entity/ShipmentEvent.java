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

import java.time.LocalDateTime;

/** A step of a shipment, deleted with the shipment. The step is a free label. */
@Entity
@Table(name = "shipment_events", indexes = @Index(name = "idx_shipment_events_ship", columnList = "shipment_id"))
@Getter
@Setter
public class ShipmentEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, foreignKey = @ForeignKey(name = "shipment_events_shipment_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Shipment shipment;

    @Column(name = "step", nullable = false, length = 30)
    private String step;

    /** The time of the step; the time it is recorded when none is sent. */
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "note", length = 200)
    private String note;
}
