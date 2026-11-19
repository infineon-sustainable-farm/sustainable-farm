package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import java.time.LocalDate;

public record OperatorAssignmentUpdateRequest(
        /** Another person of the people directory; {@code null} keeps the current one. */
        Long personId,
        Long equipmentId,
        String jobTitle,
        LocalDate startDate,
        LocalDate endDate
) {}