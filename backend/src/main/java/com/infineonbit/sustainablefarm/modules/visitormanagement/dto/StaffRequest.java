package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.core.validation.Phone;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a staff member / guide (A5).
 */
public class StaffRequest {

    @NotBlank(message = "fullName is required")
    @Size(max = 150, message = "fullName must be at most 150 characters")
    private String fullName;

    @NotNull(message = "role is required")
    private StaffRole role;

    @Email(message = "email must be a valid email address")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    @Phone
    private String phone;

    private Boolean active;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}