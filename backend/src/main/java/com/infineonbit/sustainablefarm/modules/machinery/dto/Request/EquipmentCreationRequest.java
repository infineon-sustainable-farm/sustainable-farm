package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentCreationRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or fewer") String name,
        @NotNull(message = "Category is required") Category category,
        @NotNull(message = "Stage is required") Stage stage,
        @NotNull(message = "Status is required") Status status
) {}
