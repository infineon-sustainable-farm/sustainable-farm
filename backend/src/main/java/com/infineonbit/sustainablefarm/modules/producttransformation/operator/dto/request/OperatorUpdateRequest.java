package com.infineonbit.sustainablefarm.modules.producttransformation.operator.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating an existing Operator
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorUpdateRequest {

    private String operatorName;

    private Role role;

    private String certifications;

    private ActiveStatus activeStatus;

    private LocalDate hireDate;
}
