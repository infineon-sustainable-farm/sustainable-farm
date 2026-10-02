package com.infineonbit.sustainablefarm.modules.producttransformation.operator.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.ActiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating operator active status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private ActiveStatus activeStatus;
}
