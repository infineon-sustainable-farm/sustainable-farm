package com.infineonbit.sustainablefarm.modules.producttransformation.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentSpec;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the single equipment register (ADR 0004, AC-7): machinery owns the only
 * {@code equipment} table, product transformation keeps a 1:1 extension
 * {@code pt_equipment_spec} that references it.
 *
 * <p>Plain reflection, no Spring context: the test is a contract on the mapping.</p>
 */
@DisplayName("Single equipment register (ADR 0004)")
class SingleEquipmentRegisterTest {

    private static final Class<?> MACHINERY_EQUIPMENT =
            com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment.class;

    @Test
    @DisplayName("Machinery owns the equipment table")
    void machineryOwnsTheEquipmentTable() {
        assertThat(MACHINERY_EQUIPMENT.getAnnotation(Table.class).name()).isEqualTo("equipment");
    }

    @Test
    @DisplayName("Product transformation equipment is a view of the register, not a second table")
    void productTransformationEquipmentIsNotAnEntity() {
        assertThat(Equipment.class.isAnnotationPresent(Entity.class)).isFalse();
        assertThat(Equipment.class.isAnnotationPresent(Table.class)).isFalse();
    }

    @Test
    @DisplayName("pt_equipment_spec extends the register 1:1 with a foreign key to equipment")
    void specExtendsTheRegister() {
        assertThat(EquipmentSpec.class.getAnnotation(Table.class).name()).isEqualTo("pt_equipment_spec");
        assertThat(EquipmentSpec.class.getAnnotation(Table.class).uniqueConstraints())
                .anySatisfy(unique -> assertThat(unique.columnNames()).containsExactly("equipment_id"));
        CrossModuleForeignKey foreignKey =
                new ProductTransformationEquipmentForeignKeys().equipmentSpecEquipmentForeignKey();
        assertThat(foreignKey.table()).isEqualTo("pt_equipment_spec");
        assertThat(foreignKey.column()).isEqualTo("equipment_id");
        assertThat(foreignKey.referencedTable()).isEqualTo("equipment");
        assertThat(foreignKey.onDelete()).isEqualTo(CrossModuleForeignKey.OnDelete.CASCADE);
    }
}
