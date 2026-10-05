package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

public record SparePartObtainingResponse(
        Long id,
        String name,
        Integer quantity,
        Integer reorderThreshold,
        java.math.BigDecimal unitCost,
        Long equipmentId) {
}
