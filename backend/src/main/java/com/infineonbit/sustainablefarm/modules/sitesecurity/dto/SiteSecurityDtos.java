package com.infineonbit.sustainablefarm.modules.sitesecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class SiteSecurityDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewStatsDto {
        private int activePoints;
        private int controlledZones;
        private int totalZones;
        private int visitors;
        private int alerts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GateDto {
        private String id;
        private String name;
        @com.fasterxml.jackson.annotation.JsonProperty("short")
        private String shortName;
        private String zone;
        private String status;
        private String detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewResponseDto {
        private List<GateDto> gates;
        private OverviewStatsDto stats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ZoneDto {
        private String id;
        private String name;
        private String tagline;
        private int authorizedUsers;
        private int entryPoints;
        private String status;
        private String description;
        private List<String> requirements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        private String id;
        /** The holder in the shared people directory. */
        private Long personId;
        /** The holder's full name, read from the directory; empty if the person is unknown. */
        private String name;
        private String role;
        private String type;
        private String level;
        private String validUntil;
        private String status;
        private String initials;
        private String lastActive;
        /** The holder's email, read from the directory. Only filled for the admin role. */
        @ToString.Exclude
        private String email;
        private String notificationStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class CreateUserRequestDto {
        /** The holder: a person of the shared directory, picked in GET /api/staff/persons. */
        @NotNull(message = "Person is required")
        private Long personId;

        @NotBlank(message = "Role is required")
        @Size(max = 100, message = "Role must not exceed 100 characters")
        private String role;

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "Staff|Service|Security|Visitor",
                message = "Type must be one of Staff, Service, Security or Visitor")
        private String type;

        @NotBlank(message = "Level is required")
        @Size(max = 50, message = "Level must not exceed 50 characters")
        private String level;

        @NotBlank(message = "validUntil is required")
        @Pattern(regexp = "(?i:permanent)|\\d{4}-\\d{2}-\\d{2}",
                message = "validUntil must be an ISO date (yyyy-MM-dd) or Permanent")
        private String validUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogEntryDto {
        private String id;
        private String ref;
        private String time;
        private String date;
        /** The person who passed, empty for an unidentified subject. */
        private Long personId;
        /** The person's full name, read from the directory; empty for an unidentified subject. */
        private String user;
        private String initials;
        private String type;
        private String zone;
        private String zoneName;
        private String action;
        private String status;
        private String gate;
        private String method;
        private String note;
    }
}
