package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.core.validation.Phone;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a person of the farm people directory (Staff screen), with an
 * optional visitor management role (A5).
 *
 * <p>On update, an {@code email} or {@code phone} key left out of the JSON keeps the stored
 * value; a key sent {@code null} or blank clears it (the Staff form sends the full record, so an
 * emptied field is the user's choice).</p>
 */
public class StaffRequest {

    @NotBlank(message = "fullName is required")
    @Size(max = 150, message = "fullName must be at most 150 characters")
    private String fullName;

    /** Visitor management role; {@code null} for a person who is not visitor staff (operator, technician). */
    private StaffRole role;

    @Email(message = "email must be a valid email address")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    @Phone
    private String phone;

    private Boolean active;

    /** Whether the JSON carried an {@code email} key, even {@code null}. */
    private boolean emailSent;

    /** Whether the JSON carried a {@code phone} key, even {@code null}. */
    private boolean phoneSent;

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
        this.emailSent = true;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneSent = true;
    }

    public boolean isPhoneSent() {
        return phoneSent;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}