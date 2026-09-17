package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import java.time.LocalDate;

public record OperatorAssignmentObtainingResponse(
        Long id,
        String fullName,
        Long equipmentId,
        String jobTitle,
        LocalDate startDate,
        LocalDate endDate
) {}