package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "shipment_events", indexes = @Index(name = "idx_shipment_events_ship", columnList = "shipment_id"))
public class ShipmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @NotNull(message = "shipmentId is required")
    @Column(name = "shipment_id", nullable = false)
    private Integer shipmentId;

    @NotBlank(message = "step is required")
    @Column(name = "step", nullable = false, length = 30)
    private String step;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "note", length = 200)
    private String note;

    public Integer getEventId() { return eventId; }
    public void setEventId(Integer eventId) { this.eventId = eventId; }

    public Integer getShipmentId() { return shipmentId; }
    public void setShipmentId(Integer shipmentId) { this.shipmentId = shipmentId; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}