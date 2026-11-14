package com.infineonbit.sustainablefarm.modules.sitesecurity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Requests and responses of the Farm Security accounts feature. Field names follow the API contract. */
public final class AccountDtos {

    private AccountDtos() {
    }

    private static final String ROLE = "admin|user|ADMIN|USER";
    private static final String CATEGORY = "(?i)worker|visitor|contractor|security";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        @ToString.Exclude
        private String email;

        @NotBlank(message = "Password is required")
        @Size(max = 256, message = "Password is too long")
        @ToString.Exclude
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequestDto {
        @NotBlank(message = "Token is required")
        @Size(max = 128, message = "Token is invalid")
        @ToString.Exclude
        private String token;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenPasswordRequestDto {
        @NotBlank(message = "Token is required")
        @Size(max = 128, message = "Token is invalid")
        @ToString.Exclude
        private String token;

        @NotBlank(message = "Password is required")
        @Size(max = 256, message = "Password is too long")
        @ToString.Exclude
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailOnlyRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        @ToString.Exclude
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequestDto {
        @NotBlank(message = "Current password is required")
        @Size(max = 256, message = "Password is too long")
        @ToString.Exclude
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(max = 256, message = "Password is too long")
        @ToString.Exclude
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetupPasswordRequestDto {
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}|\\d{10}", message = "Code must be 6 or 10 digits")
        @ToString.Exclude
        private String code;

        @NotBlank(message = "Password is required")
        @Size(max = 256, message = "Password is too long")
        @ToString.Exclude
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfoDto {
        private String purpose;
        private String fullName;
        private String maskedEmail;
        private Instant expiresAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InviteUserRequestDto {
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        private String email;

        @NotBlank(message = "Type the email a second time")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        private String emailConfirmation;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = ROLE, message = "Role must be admin or user")
        private String role;

        @NotBlank(message = "User type is required")
        @Pattern(regexp = CATEGORY, message = "User type must be worker, visitor, contractor or security")
        private String category;

        @Size(max = 10, message = "Too many permissions")
        private List<String> permissions;

        @Size(max = 10, message = "Too many zones")
        private List<String> zoneIds;

        private LocalDate validUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateUserRequestDto {
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        private String fullName;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = ROLE, message = "Role must be admin or user")
        private String role;

        @NotBlank(message = "User type is required")
        @Pattern(regexp = CATEGORY, message = "User type must be worker, visitor, contractor or security")
        private String category;

        @Size(max = 10, message = "Too many permissions")
        private List<String> permissions;

        @Size(max = 10, message = "Too many zones")
        private List<String> zoneIds;

        private LocalDate validUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmailStateDto {
        private String deliveryId;
        private String kind;
        private String submission;
        private String delivery;
        private int attempts;
        private Instant submittedAt;
        private String lastError;
        /** Manual attestation only (delivery = confirmed_by_admin); never a provider event. */
        private Instant deliveryConfirmedAt;
        private String deliveryConfirmedByEmail;
        private String deliveryEvidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        private String id;
        private String fullName;
        private String email;
        private String role;
        private String category;
        private List<String> permissions;
        private List<String> zoneIds;
        private LocalDate validUntil;
        private String status;
        private boolean passwordSet;
        private boolean legacyCodeSignIn;
        private Instant invitedAt;
        private String invitedBy;
        private Instant invitationExpiresAt;
        private Instant activationStartedAt;
        private Instant activatedAt;
        private Instant firstLoginAt;
        private Instant lastLoginAt;
        private Instant disabledAt;
        private String disabledBy;
        private boolean validityEnded;
        /** Latest invitation or password setup email: submission and delivery states. */
        private EmailStateDto emailDelivery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryDto {
        private long pending;
        private long activationStarted;
        private long active;
        private long expired;
        private long revoked;
        private long disabled;
        private long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopiedLinkDto {
        @ToString.Exclude
        private String link;
        private Instant expiresAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryActorDto {
        private String kind;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryEntryDto {
        private Instant at;
        private String action;
        private HistoryActorDto actor;
        private String detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeSignInConditionsDto {
        private boolean realDeliveryVerified;
        private long adminsWithPasswordSignIn;
        private long requiredAdminsWithPasswordSignIn;
        private boolean allActiveAdminsHavePassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeSignInStatusDto {
        private boolean retired;
        private CodeSignInConditionsDto conditions;
        private boolean canRetire;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireRequestDto {
        @NotNull(message = "Confirmation is required")
        private String confirmation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmDeliveryRequestDto {
        @NotBlank(message = "Describe the evidence (for example the provider log id)")
        @Size(max = 200, message = "Evidence must not exceed 200 characters")
        private String evidence;
    }
}
