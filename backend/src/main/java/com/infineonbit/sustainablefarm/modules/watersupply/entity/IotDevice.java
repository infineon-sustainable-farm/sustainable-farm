package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Registry of an IoT sensor connected to the module (level, flow, quality, rain, soil, clogging).
 * Updated at each telemetry ingestion: last_seen, battery and rssi are recomputed.
 * The backend availability indicator (P7) relies on this registry.
 */
@Entity
@Table(name = "iot_devices",
        uniqueConstraints = @UniqueConstraint(name = "iot_devices_device_id_key", columnNames = "device_id"),
        indexes = @Index(name = "idx_iot_last_seen", columnList = "last_seen"))
public class IotDevice extends BaseEntity {

    @NotBlank
    @Column(name = "device_id", nullable = false, length = 64)
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
