package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;

import java.time.LocalDate;

public record MaintenanceScheduleObtainingResponse(
        Long id,
        Long equipmentId,
        MaintenanceType type,
        String frequency,
        LocalDate lastCompleted,
        LocalDate nextDue,
        String operator) {
}
