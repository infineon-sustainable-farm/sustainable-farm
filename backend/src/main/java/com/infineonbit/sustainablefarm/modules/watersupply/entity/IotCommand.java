package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "iot_commands",
        indexes = {
                @Index(name = "idx_iot_commands_device_requested", columnList = "device_id, requested_at"),
                @Index(name = "idx_iot_commands_requested_by", columnList = "requested_by")})
public class IotCommand extends BaseEntity {
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** Read-only link that declares the foreign key fk_iot_commands_device; the code writes the device_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", referencedColumnName = "device_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_iot_commands_device"))
    private IotDevice device;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IotCommandType commandType;
    @Column(columnDefinition = "text")
    private String payload;
    @Convert(converter = IotCommandStatus.Converter.class)
    @Column(nullable = false, length = 32)
    private IotCommandStatus status;
    private Long requestedBy;
    @Column(nullable = false)
    private Instant requestedAt;
    private Instant acknowledgedAt;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public IotCommandType getCommandType() { return commandType; }
    public void setCommandType(IotCommandType commandType) { this.commandType = commandType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public IotCommandStatus getStatus() { return status; }
    public void setStatus(IotCommandStatus status) { this.status = status; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
