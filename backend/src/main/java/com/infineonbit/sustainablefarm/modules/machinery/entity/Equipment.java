package com.infineonbit.sustainablefarm.modules.machinery.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import jakarta.persistence.*;
import lombok.*;

/**
 * The single equipment register of the farm (ADR 0004): machinery owns it, the other
 * modules read and write it through {@code modules.machinery.api}.
 *
 * <p>{@code code} is the readable business code next to the Long id (for example
 * {@code EQ-00012}, or the code chosen when product transformation registers its
 * processing equipment).</p>
 */
@Entity
@Table(name = "equipment", uniqueConstraints = {
        @UniqueConstraint(name = "equipment_name_key", columnNames = "name"),
        @UniqueConstraint(name = "equipment_code_key", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
public class Equipment extends BaseEntity {

    public static final int CODE_MAX_LENGTH = 50;
    public static final int NAME_MAX_LENGTH = 100;

    @Column(length = CODE_MAX_LENGTH, nullable = false)
    private String code;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
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

    public Equipment(Long id, String name, Category category, Stage stage, Status status) {
        setId(id);
        this.name = name;
        this.category = category;
        this.stage = stage;
        this.status = status;
    }
}
