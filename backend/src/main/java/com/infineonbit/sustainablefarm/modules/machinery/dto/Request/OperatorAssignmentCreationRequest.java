package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OperatorAssignmentCreationRequest(
        /** Person of the people directory (GET /api/staff/persons) who operates the equipment. */
        @NotNull(message = "Operator is required") Long personId,
        @NotNull(message = "Equipment id is required") Long equipmentId,
        @NotBlank(message = "Operator's job title is required") String jobTitle,
        @NotNull(message = "Start date is required") LocalDate startDate,
        LocalDate endDate
) {}