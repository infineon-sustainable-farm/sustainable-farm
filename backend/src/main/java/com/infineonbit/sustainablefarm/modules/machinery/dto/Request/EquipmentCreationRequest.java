package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import jakarta.validation.constraints.NotBlank;

public record EquipmentCreationRequest(
        @NotBlank String name,
        @NotBlank Category category, // Change later in NotNull
        @NotBlank Stage stage,
        @NotBlank Status status
) {}
