package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to mark a briefing as delivered.
 */
public class BriefingDeliverRequest {

    /** Person of the people directory who delivered the briefing. */
    @NotNull(message = "staffMemberPersonId is required")
    private Long staffMemberPersonId;

    @Size(max = 500)
    private String signature;

    public Long getStaffMemberPersonId() {
        return staffMemberPersonId;
    }

    public void setStaffMemberPersonId(Long staffMemberPersonId) {
        this.staffMemberPersonId = staffMemberPersonId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
