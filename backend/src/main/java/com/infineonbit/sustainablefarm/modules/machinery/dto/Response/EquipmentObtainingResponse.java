package com.infineonbit.sustainablefarm.modules.machinery.dto.Response;

import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;

public record EquipmentObtainingResponse(
        Long id,
        String name,
        Category category,
        Stage stage,
        Status status) {
}
