package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;

public record EquipmentObtainingResponse(
        Long id,
        String name,
        Category category,
        Stage stage,
        Status status,
        String code) {
}
