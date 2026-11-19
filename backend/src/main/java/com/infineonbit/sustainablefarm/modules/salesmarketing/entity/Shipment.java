package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The shipment of an order, with its live tracking figures. An order that is shipped cannot be deleted. */
@Entity
@Table(name = "shipments", uniqueConstraints = @UniqueConstraint(name = "shipments_shipment_code_key", columnNames = "shipment_code"),
        indexes = @Index(name = "idx_shipments_order", columnList = "order_id"))
@Getter
@Setter
public class Shipment extends BaseEntity {

    @Column(name = "shipment_code", length = 20)
    private String shipmentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "shipments_order_id_fkey"))
    private Order order;

    @Column(name = "origin", length = 100)
    private String origin;

    @Column(name = "destination", length = 100)
    private String destination;

    /** {@code packed} when none is sent. */
    @ColumnDefault("'packed'")
    @Convert(converter = ShipmentStatus.Converter.class)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(name = "eta")
    private LocalDate eta;

    @Column(name = "vessel_name", length = 100)
    private String vesselName;

    @Column(name = "voyage_number", length = 30)
    private String voyageNumber;

    @Column(name = "delay_reason", length = 200)
    private String delayReason;

    @Column(name = "current_lat", precision = 9, scale = 6)
    private BigDecimal currentLat;

    @Column(name = "current_lon", precision = 9, scale = 6)
    private BigDecimal currentLon;

    @Column(name = "speed_knots", precision = 5, scale = 1)
    private BigDecimal speedKnots;

    @Column(name = "current_location_label", length = 150)
    private String currentLocationLabel;

    @Column(name = "distance_remaining_nm", precision = 8, scale = 1)
    private BigDecimal distanceRemainingNm;

    @Column(name = "commodity_description", length = 150)
    private String commodityDescription;

    @Column(name = "container_count")
    private Integer containerCount;

    @Column(name = "container_type", length = 20)
    private String containerType;

    @Column(name = "cargo_weight_kg", precision = 10, scale = 1)
    private BigDecimal cargoWeightKg;

    @Column(name = "current_temp_c", precision = 4, scale = 1)
    private BigDecimal currentTempC;

    @Column(name = "temp_status", length = 20)
    private String tempStatus;

    @Column(name = "manifest_url")
    private String manifestUrl;
}
