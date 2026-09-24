package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EquipmentCreationRequest(
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Category is required") Category category,
        @NotNull(message = "Stage is required") Stage stage,
        @NotNull(message = "Status is required") Status status
) {}
