package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Briefing;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BriefingStatus;

import java.time.Instant;

/**
 * API representation of a safety briefing.
 */
public class BriefingResponse {

    private Long id;
    private Long registrationId;
    private Instant deliveredAt;
    private String staffMember;
    private String signature;
    private BriefingStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static BriefingResponse from(Briefing b) {
        BriefingResponse r = new BriefingResponse();
        r.id = b.getId();
        r.registrationId = b.getRegistration().getId();
        r.deliveredAt = b.getDeliveredAt();
        r.staffMember = b.getStaffMember();
        r.signature = b.getSignature();
        r.status = b.getStatus();
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getStaffMember() {
        return staffMember;
    }

    public String getSignature() {
        return signature;
    }

    public BriefingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public void setStaffMember(String staffMember) { this.staffMember = staffMember; }
    public void setSignature(String signature) { this.signature = signature; }
    public void setStatus(BriefingStatus status) { this.status = status; }
}
