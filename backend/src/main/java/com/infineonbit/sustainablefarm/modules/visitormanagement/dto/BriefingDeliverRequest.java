package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to mark a briefing as delivered.
 */
public class BriefingDeliverRequest {

    @NotBlank(message = "staffMember is required")
    @Size(max = 150)
    private String staffMember;

    @Size(max = 500)
    private String signature;

    public String getStaffMember() {
        return staffMember;
    }

    public void setStaffMember(String staffMember) {
        this.staffMember = staffMember;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
