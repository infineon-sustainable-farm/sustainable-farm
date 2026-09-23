package com.sustainablefarm.modules.producttransformation.resources.operator.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator.ActiveStatus;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new Operator
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorCreateRequest {

    @NotBlank(message = "Operator ID is required")
    private String operatorId;

    @NotBlank(message = "Operator name is required")
    private String operatorName;

    @NotNull(message = "Role is required")
    private Role role;

    private String certifications;

    private ActiveStatus activeStatus;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;
}
