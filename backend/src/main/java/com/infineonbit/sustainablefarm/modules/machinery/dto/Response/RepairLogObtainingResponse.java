package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairLogObtainingResponse(
        Long id,
        Long equipmentId,
        LocalDate date,
        String issue,
        BigDecimal downtime,
        BigDecimal cost,
        String technician) {
}
