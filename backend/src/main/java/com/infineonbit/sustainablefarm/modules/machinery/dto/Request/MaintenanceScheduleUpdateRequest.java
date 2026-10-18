package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceScheduleUpdateRequest(
        Long equipmentId,

        MaintenanceType type,

        @Size(max = 100, message = "Frequency must be 100 characters or fewer")
        String frequency,

        LocalDate lastCompleted,

        LocalDate nextDue,

        @Size(max = 60, message = "Operator must be 60 characters or fewer")
        String operator
) {}
