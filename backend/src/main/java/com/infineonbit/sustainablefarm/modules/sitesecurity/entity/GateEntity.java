package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_security_gates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GateEntity {
    @Id
    private String id;
    private String name;
    private String shortName;
    private String zone;
    private String status;
    private String detail;
}
