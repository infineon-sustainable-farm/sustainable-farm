package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SparePartCreationRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be non-negative")
        Integer quantity,

        @NotNull(message = "Reorder threshold is required")
        @Min(value = 0, message = "Reorder threshold must be non-negative")
        Integer reorderThreshold,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.00", message = "Unit cost must be at least 0.00")
        java.math.BigDecimal unitCost,

        Long equipmentId
) {}
