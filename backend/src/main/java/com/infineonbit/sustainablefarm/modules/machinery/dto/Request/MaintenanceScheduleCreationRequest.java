package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceScheduleCreationRequest(
        @NotNull(message = "Equipment is required")
        Long equipmentId,

        @NotNull(message = "Maintenance type is required")
        MaintenanceType type,

        @Size(max = 100, message = "Frequency must be 100 characters or fewer")
        String frequency,

        @NotNull(message = "Last completed date is required")
        LocalDate lastCompleted,

        @NotNull(message = "Next due date is required")
        LocalDate nextDue,

        /** Person of the people directory (GET /api/staff/persons); {@code null} for none. */
        Long operatorPersonId
) {}
