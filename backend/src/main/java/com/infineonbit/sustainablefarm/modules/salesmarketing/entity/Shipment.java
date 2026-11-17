package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "shipments", uniqueConstraints = @UniqueConstraint(name = "shipments_shipment_code_key", columnNames = "shipment_code"),
        indexes = @Index(name = "idx_shipments_order", columnList = "order_id"))
@Check(name = "shipments_status_check", constraints = "status IN ('packed', 'shipped', 'port', 'in_transit', 'delivered', 'delayed')")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Integer shipmentId;

    @Column(name = "shipment_code", length = 20)
    private String shipmentCode;

    @Column(name = "order_id")
    private Integer orderId;

    @NotBlank(message = "origin is required")
    @Column(name = "origin", length = 100)
    private String origin;

    @NotBlank(message = "destination is required")
    @Column(name = "destination", length = 100)
    private String destination;

    @ColumnDefault("'packed'")
    @Column(name = "status", length = 30)
    private String status;

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

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getShipmentId() { return shipmentId; }
    public void setShipmentId(Integer shipmentId) { this.shipmentId = shipmentId; }

    public String getShipmentCode() { return shipmentCode; }
    public void setShipmentCode(String shipmentCode) { this.shipmentCode = shipmentCode; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEta() { return eta; }
    public void setEta(LocalDate eta) { this.eta = eta; }

    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }

    public String getVoyageNumber() { return voyageNumber; }
    public void setVoyageNumber(String voyageNumber) { this.voyageNumber = voyageNumber; }

    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String delayReason) { this.delayReason = delayReason; }

    public BigDecimal getCurrentLat() { return currentLat; }
    public void setCurrentLat(BigDecimal currentLat) { this.currentLat = currentLat; }

    public BigDecimal getCurrentLon() { return currentLon; }
    public void setCurrentLon(BigDecimal currentLon) { this.currentLon = currentLon; }

    public BigDecimal getSpeedKnots() { return speedKnots; }
    public void setSpeedKnots(BigDecimal speedKnots) { this.speedKnots = speedKnots; }

    public String getCurrentLocationLabel() { return currentLocationLabel; }
    public void setCurrentLocationLabel(String currentLocationLabel) { this.currentLocationLabel = currentLocationLabel; }

    public BigDecimal getDistanceRemainingNm() { return distanceRemainingNm; }
    public void setDistanceRemainingNm(BigDecimal distanceRemainingNm) { this.distanceRemainingNm = distanceRemainingNm; }

    public String getCommodityDescription() { return commodityDescription; }
    public void setCommodityDescription(String commodityDescription) { this.commodityDescription = commodityDescription; }

    public Integer getContainerCount() { return containerCount; }
    public void setContainerCount(Integer containerCount) { this.containerCount = containerCount; }

    public String getContainerType() { return containerType; }
    public void setContainerType(String containerType) { this.containerType = containerType; }

    public BigDecimal getCargoWeightKg() { return cargoWeightKg; }
    public void setCargoWeightKg(BigDecimal cargoWeightKg) { this.cargoWeightKg = cargoWeightKg; }

    public BigDecimal getCurrentTempC() { return currentTempC; }
    public void setCurrentTempC(BigDecimal currentTempC) { this.currentTempC = currentTempC; }

    public String getTempStatus() { return tempStatus; }
    public void setTempStatus(String tempStatus) { this.tempStatus = tempStatus; }

    public String getManifestUrl() { return manifestUrl; }
    public void setManifestUrl(String manifestUrl) { this.manifestUrl = manifestUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}