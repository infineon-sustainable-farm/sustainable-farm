package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_security_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneEntity {
    @Id
    private String id;
    private String name;
    private String tagline;
    private int authorizedUsers;
    private int entryPoints;
    private String status;
    private String description;

    @ElementCollection
    private List<String> requirements;
}
