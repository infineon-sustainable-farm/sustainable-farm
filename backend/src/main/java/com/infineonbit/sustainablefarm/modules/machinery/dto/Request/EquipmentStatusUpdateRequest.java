package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import jakarta.validation.constraints.NotNull;

public record EquipmentStatusUpdateRequest(
        @NotNull(message = "Status is required") Status status
) {
}
