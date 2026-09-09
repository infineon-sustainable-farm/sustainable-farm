package com.infineonbit.sustainablefarm.modules.sitesecurity.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SiteSecurityDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewStatsDto {
        private int activePoints;
        private int controlledZones;
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
        private String name;
        private String role;
        private String type;
        private String level;
        private String validUntil;
        private String status;
        private String initials;
        private String lastActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateUserRequestDto {
        private String name;
        private String role;
        private String type;
        private String level;
        private String validUntil;
        private String status;
        private String initials;
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
