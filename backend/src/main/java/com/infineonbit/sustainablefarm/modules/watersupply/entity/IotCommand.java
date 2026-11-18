package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "iot_commands",
        indexes = @Index(name = "idx_iot_commands_device_requested", columnList = "device_id, requested_at"))
public class IotCommand extends BaseEntity {
    @Column(nullable = false, length = 64)
    private String deviceId;
    @Column(nullable = false, length = 64)
    private String commandType;
    @Column(columnDefinition = "text")
    private String payload;
    @Column(nullable = false, length = 32)
    private String status;
    private Long requestedBy;
    @Column(nullable = false)
    private Instant requestedAt;
    private Instant acknowledgedAt;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
