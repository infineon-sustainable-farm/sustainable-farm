package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import java.time.LocalDate;

public record OperatorAssignmentUpdateRequest(
        String fullName,
        Long equipmentId,
        String jobTitle,
        LocalDate startDate,
        LocalDate endDate
) {}