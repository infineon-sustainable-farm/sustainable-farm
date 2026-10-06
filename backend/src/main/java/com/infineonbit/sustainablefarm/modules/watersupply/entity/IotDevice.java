package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

/**
 * Registre d'un capteur IoT connecté au module (niveau, débit, qualité, pluie, sol, colmatage).
 * Mis à jour à chaque ingestion de télémétrie : last_seen, battery et rssi sont recalculés.
 * L'indicateur de disponibilité backend (P7) se base sur ce registre.
 */
@Entity
@Table(name = "iot_devices")
public class IotDevice extends BaseEntity {

    @NotBlank
    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceUid;

    @NotBlank
    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "battery_percent")
    private Integer batteryPercent;

    @Column(name = "rssi")
    private Integer rssi;

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Integer getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Integer batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }
}
