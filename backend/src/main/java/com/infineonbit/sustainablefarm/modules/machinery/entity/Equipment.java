package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment", uniqueConstraints = @UniqueConstraint(name = "equipment_name_key", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Status status;
}
