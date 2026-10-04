package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record SparePartUpdateRequest(
        String name,

        @Min(value = 0, message = "Quantity must be non-negative")
        Integer quantity,

        @Min(value = 0, message = "Reorder threshold must be non-negative")
        Integer reorderThreshold,

        @DecimalMin(value = "0.00", message = "Unit cost must be at least 0.00")
        java.math.BigDecimal unitCost,

        Long equipmentId
) {}
