package com.infineonbit.sustainablefarm.modules.producttransformation.operator.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Role;
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

    /**
     * Person of the people directory (GET /api/staff/persons) who receives the operator role; the
     * operator name is the person's.
     */
    @NotNull(message = "Person is required")
    private Long personId;

    @NotNull(message = "Role is required")
    private Role role;

    private String certifications;

    private ActiveStatus activeStatus;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;
}
