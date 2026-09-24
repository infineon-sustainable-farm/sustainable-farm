package com.sustainablefarm.modules.producttransformation.resources.operator.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding a certification to an operator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorCertificationRequest {

    @NotBlank(message = "Certification is required")
    private String certification;
}
