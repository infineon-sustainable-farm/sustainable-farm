package com.sustainablefarm.dto.response;

import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for Operator response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorResponse {

    private String operatorId;
    private String operatorName;
    private Role role;
    private String certifications;
    private ActiveStatus activeStatus;
    private LocalDate hireDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
