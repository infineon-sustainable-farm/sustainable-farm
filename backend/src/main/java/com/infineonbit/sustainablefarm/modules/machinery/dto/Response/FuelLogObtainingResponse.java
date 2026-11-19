package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelLogObtainingResponse(
        Long id,
        Long equipmentId,
        LocalDate date,
        BigDecimal liters,
        BigDecimal cost) {
}
