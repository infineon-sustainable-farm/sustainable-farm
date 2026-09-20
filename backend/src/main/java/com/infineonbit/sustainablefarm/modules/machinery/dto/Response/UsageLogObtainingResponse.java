package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageLogObtainingResponse(
        Long id,
        Long equipmentId,
        LocalDate date,
        BigDecimal hoursUsed,
        String operator,
        String notes) {
}
